#include <WiFi.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>

// --- NETWORK SETTINGS ---
const char* ssid = "Catherine's Pixel";
const char* pass = "y778c3rqcgjjf95";

IPAddress serverAddress(10,103,59,218);
int serverPort = 8080;

WiFiClient client;

// --- PIN DEFINITIONS ---
#define SERVO_PIN 27
#define BUZZER_PIN 21
#define ULTRASONIC_PIN 33

// --- TIMINGS ---
const unsigned long TIME_WAIT_AFTER_DISPENSE = 3000;
const unsigned long TIME_ALARM_DURATION = 5000;
const unsigned long TIME_SNOOZE_DURATION = 3000;
const float DEGREES = 98;

// --- OBJECTS ---
Servo myServo;

// --- POLLING ---
unsigned long lastPollTime = 0;
const long POLL_INTERVAL = 2000;

// --- BUZZER CHANNEL ---
#define BUZZER_CHANNEL 4

// --- INTAKE IDS ---
const int MAX_INTAKE_IDS = 10;
int lastDispenseIntakeIds[MAX_INTAKE_IDS];
int lastDispenseIntakeIdsCount = 0;

// ---------------------------------------------------------
// SETUP
// ---------------------------------------------------------

void setup() {
  Serial.begin(115200);

  // Servo
  myServo.attach(SERVO_PIN, 500, 2500);
  myServo.write(0);

  // Buzzer
  ledcSetup(BUZZER_CHANNEL, 2000, 8);
  ledcAttachPin(BUZZER_PIN, BUZZER_CHANNEL);

  // WiFi
  Serial.print("Connecting to WiFi...");
  WiFi.begin(ssid, pass);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  Serial.println("\nConnected!");
  Serial.println(WiFi.localIP());
}

// ---------------------------------------------------------
// LOOP
// ---------------------------------------------------------

void loop() {
  if (millis() - lastPollTime > POLL_INTERVAL) {
    checkServerForCommands();
    lastPollTime = millis();
  }
}

// ---------------------------------------------------------
// DISPENSE FLOW (UPDATED LOGIC)
// ---------------------------------------------------------

void runDispenseCycle() {
  Serial.println("--- Dispense Cycle Started ---");
  long before = getStableDistance();
  Serial.print("Before dispense: ");
  Serial.println(before);

  // 1. Dispense
  dispenseOnePill();

  // 2. Let pill settle
  delay(TIME_WAIT_AFTER_DISPENSE);

  // 3. Take baseline measurement
  long baseline = getStableDistance();
  Serial.print("Baseline distance: ");
  Serial.println(baseline);

  while (true) {

    long current = getStableDistance();

    Serial.print("Current distance: ");
    Serial.println(current);

    // Detect pill removal (distance increases)
    if (current > baseline) {
      Serial.println("Pill removed!");
      sendStatusReport(true);
      break;
    }

    Serial.println("Pill still present → alarm");

    bool snoozed = ringBuzzerWithServerCheck(TIME_ALARM_DURATION);

    if (snoozed) {
      Serial.println("Snoozed...");
      ledcWriteTone(BUZZER_CHANNEL, 0);
      delay(TIME_SNOOZE_DURATION);
    } else {
      Serial.println("Missed dose.");
      ledcWriteTone(BUZZER_CHANNEL, 0);
      sendStatusReport(false);
      break;
    }
  }
}

// ---------------------------------------------------------
// ULTRASONIC
// ---------------------------------------------------------

long getUltrasonicCM() {
  pinMode(ULTRASONIC_PIN, OUTPUT);
  digitalWrite(ULTRASONIC_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(ULTRASONIC_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(ULTRASONIC_PIN, LOW);

  pinMode(ULTRASONIC_PIN, INPUT);
  long duration = pulseIn(ULTRASONIC_PIN, HIGH, 30000);

  if (duration == 0) return -1;
  return duration / 29 / 2;
}

// New: stable averaged reading
long getStableDistance() {
  int valid = 0;
  long total = 0;

  for (int i = 0; i < 5; i++) {
    long d = getUltrasonicCM();
    if (d > 0) {
      total += d;
      valid++;
    }
    delay(50);
  }

  if (valid == 0) return -1;
  return total / valid;
}

// ---------------------------------------------------------
// BUZZER + SERVER SNOOZE
// ---------------------------------------------------------

bool ringBuzzerWithServerCheck(unsigned long duration) {
  unsigned long start = millis();
  unsigned long lastBeep = 0;
  bool beepState = false;

  while (millis() - start < duration) {

    if (millis() - lastBeep > 200) {
      lastBeep = millis();
      beepState = !beepState;

      if (beepState) {
        ledcWriteTone(BUZZER_CHANNEL, 2000);
      } else {
        ledcWriteTone(BUZZER_CHANNEL, 0);
      }
    }

    if (millis() % 1000 < 20) {
      if (checkForSnoozeCommand()) return true;
    }

    delay(10);
  }

  return false;
}

// ---------------------------------------------------------
// NETWORK
// ---------------------------------------------------------

void checkServerForCommands() {
  if (client.connect(serverAddress, serverPort)) {
    client.println("GET /api/firmware/poll?deviceId=DISPENSER_001 HTTP/1.1");
    client.print("Host: "); client.println(serverAddress);
    client.println("Connection: close");
    client.println();

    String responseBody = "";
    bool headerEnded = false;

    while (client.connected() || client.available()) {
      if (client.available()) {
        String line = client.readStringUntil('\n');
        if (line == "\r") headerEnded = true;
        else if (headerEnded) responseBody += line;
      }
    }

    client.stop();

    int jsonStart = responseBody.indexOf('{');
    int jsonEnd = responseBody.lastIndexOf('}');

    if (jsonStart >= 0 && jsonEnd > jsonStart) {
      String jsonOnly = responseBody.substring(jsonStart, jsonEnd + 1);

      StaticJsonDocument<200> doc;
      if (!deserializeJson(doc, jsonOnly)) {
        const char* cmd = doc["command"];

        if (strcmp(cmd, "DISPENSE") == 0) {

          lastDispenseIntakeIdsCount = 0;
          JsonArray arr = doc["intakeIds"];

          for (int i = 0; i < arr.size() && i < MAX_INTAKE_IDS; i++) {
            lastDispenseIntakeIds[lastDispenseIntakeIdsCount++] = arr[i];
          }

          runDispenseCycle();
        }
      }
    }
  }
}

bool checkForSnoozeCommand() {
  if (client.connect(serverAddress, serverPort)) {
    client.println("GET /api/firmware/poll HTTP/1.1");
    client.print("Host: "); client.println(serverAddress);
    client.println("Connection: close");
    client.println();

    String response = "";

    while (client.connected() || client.available()) {
      if (client.available()) {
        response += client.readString();
      }
    }

    client.stop();

    return response.indexOf("SNOOZE") >= 0;
  }
  return false;
}

void sendStatusReport(bool wasTaken) {
  if (client.connect(serverAddress, serverPort)) {
    StaticJsonDocument<256> doc;

    JsonArray ids = doc.createNestedArray("intakeIds");
    for (int i = 0; i < lastDispenseIntakeIdsCount; i++) {
      ids.add(lastDispenseIntakeIds[i]);
    }

    doc["dispenseEventStatus"] = wasTaken;

    String json;
    serializeJson(doc, json);

    client.println("POST /api/firmware/status HTTP/1.1");
    client.print("Host: "); client.println(serverAddress);
    client.println("Content-Type: application/json");
    client.print("Content-Length: "); client.println(json.length());
    client.println("Connection: close");
    client.println();
    client.println(json);

    client.stop();
    lastDispenseIntakeIdsCount = 0;
  }
}

void dispenseOnePill() {
  // Pulse the servo multiple times to advance exactly one compartment
  // 14 compartments = need 1/14 of a full rotation per dispense
  // Tune PULSE_DURATION_MS and NUM_PULSES to the wheel

  const int NUM_PULSES = 1;          // number of short bursts
  const int PULSE_DURATION_MS = 500;  // ms per burst — tune
  const int PULSE_GAP_MS = 50;       // pause between bursts

  for (int i = 0; i < NUM_PULSES; i++) {
    myServo.write(98);           // working speed
    delay(PULSE_DURATION_MS);
    myServo.write(0);           // stop
    delay(PULSE_GAP_MS);
  }
}
