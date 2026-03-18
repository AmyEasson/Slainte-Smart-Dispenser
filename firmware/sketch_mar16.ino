#include <Servo.h>
#include <SPI.h>
#include <WiFiNINA.h>
#include <ArduinoJson.h>

// --- NETWORK SETTINGS ---
char ssid[] = "acheesylion";      // Your Network Name
char pass[] = "batman123";               // Your Network Password
int status = WL_IDLE_STATUS;

// IP Address of your computer (Laptop on Hotspot)
IPAddress serverAddress(172, 20, 10, 4);
int serverPort = 8080;

WiFiClient client;

// --- HARDWARE PIN DEFINITIONS ---
const int IR_SENSOR = 0;       // Pin A0
const int MOTOR = 9;           // Pin ~9
const int BUZZER = 8;          // Pin 8
const int PHYSICAL_START = 2;  // Pin 2 (Button 1)
const int PHYSICAL_SNOOZE = 3; // Pin ~3 (Button 2)

// --- TIMINGS ---
const unsigned long TIME_WAIT_AFTER_DISPENSE = 3000; 
const unsigned long TIME_READ_SENSOR = 2000;         
const unsigned long TIME_ALARM_DURATION = 5000;      
const unsigned long TIME_SNOOZE_DURATION = 3000;     
const float DEGREES = 98;
const int SENSOR_THRESHOLD = 50; 

// --- OBJECTS ---
Servo myServo;

// --- POLLING TIMER ---
unsigned long lastPollTime = 0;
const long POLL_INTERVAL = 2000;

// --- LAST DISPENSE INTAKE IDS (from poll response; used in status report) ---
const int MAX_INTAKE_IDS = 10;
int lastDispenseIntakeIds[MAX_INTAKE_IDS];
int lastDispenseIntakeIdsCount = 0; 

void setup() {
  Serial.begin(9600);
  
  // Hardware Setup
  myServo.attach(MOTOR);
  myServo.write(0); 
  pinMode(BUZZER, OUTPUT);
  
  // Button Setup (INPUT_PULLUP: LOW when pressed)
  pinMode(PHYSICAL_START, INPUT_PULLUP);
  pinMode(PHYSICAL_SNOOZE, INPUT_PULLUP);

  // WiFi Setup
  Serial.print("Connecting to WiFi...");
  while (status != WL_CONNECTED) {
    Serial.print(".");
    status = WiFi.begin(ssid, pass);
    delay(1000);
  }
  Serial.println("\nConnected to WiFi!");
  printWiFiStatus();
}

void loop() {
  // 1. Check Physical Button (Start)
  if (digitalRead(PHYSICAL_START) == LOW) {
    Serial.println("Physical Start Button Pressed!");
    runDispenseCycle();
    delay(1000); // Debounce
  }

  // 2. Poll Server (every 2 seconds)
  if (millis() - lastPollTime > POLL_INTERVAL) {
    checkServerForCommands();
    lastPollTime = millis();
  }
}

// ---------------------------------------------------------
// HYBRID LOGIC FLOW
// ---------------------------------------------------------

void runDispenseCycle() {
  Serial.println("--- Dispense Cycle Started ---");
  
  // 1. Dispense
  myServo.write(DEGREES);
  delay(1000); 
  myServo.write(0);
  
  // 2. Wait
  delay(TIME_WAIT_AFTER_DISPENSE);

  bool pillTaken = false;
  
  // 3. Monitor Loop
  while (!pillTaken) {
    bool pillDetected = checkSensorForTime(TIME_READ_SENSOR);
    
    if (!pillDetected) {
      Serial.println("Tray Empty. Pill Taken.");
      sendStatusReport(true); 
      pillTaken = true;
    } else {
      Serial.println("Pill Detected. Alarm!");
      
      // Ring Buzzer (Checks BOTH Physical Button AND Network)
      bool snoozed = ringBuzzerHybrid(TIME_ALARM_DURATION);
      
      if (snoozed) {
        Serial.println("Snooze Triggered (Physical or WiFi). Pausing...");
        noTone(BUZZER);
        //digitalWrite(BUZZER, LOW); // Stop Sound
        delay(TIME_SNOOZE_DURATION);
      } else {
        Serial.println("Alarm finished (Missed Dose).");
        noTone(BUZZER);
        //digitalWrite(BUZZER, LOW); // Stop Sound
        sendStatusReport(false); 
        break;
      }
    }
  }
}

// ---------------------------------------------------------
// HYBRID HELPER FUNCTIONS
// ---------------------------------------------------------

// Checks BOTH the Physical Button AND the Network for snooze command
bool ringBuzzerHybrid(unsigned long duration) {
  unsigned long start = millis();
  unsigned long lastBeep = 0;
  bool beepState = false;

  while (millis() - start < duration) {
    // UPDATED BEEP LOGIC FOR LOUDER SOUND
    // We create a "Beep-Beep-Beep" effect using tone()
    if (millis() - lastBeep > 200) { // Changed to 200ms for a clearer beep rhythm
      lastBeep = millis();
      beepState = !beepState;
      
      if (beepState) {
        // Play 2000Hz tone (Standard "Loud" BEEP frequency)
        tone(BUZZER, 2000); 
      } else {
        // Silence
        noTone(BUZZER); 
      }
    }

    // CHECK 1: Physical Snooze Button
    if (digitalRead(PHYSICAL_SNOOZE) == LOW) {
       return true; 
    }

    // CHECK 2: Network Snooze Command (Check every ~1 sec)
    if (millis() % 1000 < 20) { 
       if (checkForSnoozeCommand()) return true;
    }

    // CHECK 3: Pill Removed?
    if (analogRead(IR_SENSOR) <= SENSOR_THRESHOLD) {
       // Ideally we would return here, but for now we let the loop continue
    }

    delay(10);
  }
  return false;
}

bool checkSensorForTime(unsigned long duration) {
  unsigned long start = millis();
  int detections = 0;
  while (millis() - start < duration) {
    if (analogRead(IR_SENSOR) > SENSOR_THRESHOLD) detections++;
    delay(100);
  }
  return (detections > 2);
}

// ---------------------------------------------------------
// NETWORK FUNCTIONS
// ---------------------------------------------------------

void checkServerForCommands() {
  if (client.connect(serverAddress, serverPort)) {
    client.println("GET /api/firmware/poll?deviceId=DISPENSER_001 HTTP/1.1");
    client.print("Host: "); client.println(serverAddress);
    client.println("Connection: close");
    client.println();

    String responseBody = "";
    boolean headerEnded = false;
    
    while (client.connected() || client.available()) {
      if (client.available()) {
        String line = client.readStringUntil('\n');
        if (line == "\r") headerEnded = true; 
        else if (headerEnded) responseBody += line;
        
      }
    }
    client.stop(); 

    responseBody.trim();
   
    int jsonStart = responseBody.indexOf('{');
    int jsonEnd = responseBody.lastIndexOf('}'); 
    if (jsonStart >= 0 && jsonEnd > jsonStart) {
      String jsonOnly =responseBody.substring(jsonStart, jsonEnd + 1); 
      StaticJsonDocument<200> doc;
      DeserializationError error = deserializeJson(doc, jsonOnly);

      if (!error) {
        const char* cmd = doc["command"];
        if (strcmp(cmd, "IDLE") != 0) {
           Serial.print("Server Command: "); Serial.println(cmd);
        }
        
        if (strcmp(cmd, "DISPENSE") == 0) {
          // Store intake ids from poll response for use in sendStatusReport()
          lastDispenseIntakeIdsCount = 0;
          JsonArray intakeIdsArr = doc["intakeIds"];
          if (!intakeIdsArr.isNull()) {
            for (size_t i = 0; i < intakeIdsArr.size() && i < (size_t)MAX_INTAKE_IDS; i++) {
              lastDispenseIntakeIds[lastDispenseIntakeIdsCount++] = (int)intakeIdsArr[i];
            }
          }
          runDispenseCycle();
        }

        // runs when the user un-pauses the dispensing, to get rid of the missed pills
        if (strcmp(cmd, "ADVANCE") == 0){
            Serial.println("Advancing carousel (missed slot)...");
            myServo.write(DEGREES);
            delay(1000);
            myServo.write(0);
            delay(TIME_WAIT_AFTER_DISPENSE);
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

    String responseBody = "";
    boolean headerEnded = false;
    while (client.connected() || client.available()) {
      if (client.available()) {
        String line = client.readStringUntil('\n');
        if (line == "\r") headerEnded = true;
        else if (headerEnded) responseBody += line;
      }
    }
    client.stop();
    
    if (responseBody.indexOf("SNOOZE") >= 0) {
      return true;
    }
  }
  return false;
}

void sendStatusReport(bool wasTaken) {
  if (client.connect(serverAddress, serverPort)) {
    Serial.println("Sending Report to Server...");
    StaticJsonDocument<256> doc;
    JsonArray ids = doc.createNestedArray("intakeIds");
    for (int i = 0; i < lastDispenseIntakeIdsCount; i++) {
      ids.add(lastDispenseIntakeIds[i]);
    }
    doc["dispenseEventStatus"] = wasTaken;

    String jsonString;
    serializeJson(doc, jsonString);

    client.println("POST /api/firmware/status HTTP/1.1");
    client.print("Host: "); client.println(serverAddress);
    client.println("Content-Type: application/json");
    client.print("Content-Length: "); client.println(jsonString.length());
    client.println("Connection: close");
    client.println();
    client.println(jsonString);
    client.stop();

    // Clear so we don't reuse ids (e.g. next physical-button dispense reports no ids)
    lastDispenseIntakeIdsCount = 0;
  }
}

void printWiFiStatus() {
  Serial.print("SSID: "); Serial.println(WiFi.SSID());
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: "); Serial.println(ip);
}