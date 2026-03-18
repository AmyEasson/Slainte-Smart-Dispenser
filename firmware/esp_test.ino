// ============================================================
//  HUZZAH32 Feather -- Full Hardware Diagnostic
//
//  Tests all components simultaneously. Open Serial Monitor
//  at 115200 baud. Type 'c' and press Enter to stop all tests.
//
//  Pin Map:
//    GPIO 33  - Grove Ultrasonic Sensor (SIG)
//    GPIO 27  - Servo Motor (signal)
//    GPIO 21  - Buzzer
//    GPIO 15  - Grove Start Button (SIG)
//    GPIO 14  - Grove Snooze Button (SIG)
// ============================================================

#include <ESP32Servo.h>

// --- Pin Definitions ---
#define ULTRASONIC_PIN  33
#define SERVO_PIN       27
#define BUZZER_PIN      21
#define START_BTN_PIN   15
#define SNOOZE_BTN_PIN  14

// --- Objects ---
Servo myServo;

// --- State ---
bool running = true;
unsigned long lastPrint = 0;
const unsigned long PRINT_INTERVAL = 500;

// ============================================================
//  Grove Ultrasonic Ranger -- single-pin trigger + echo
// ============================================================
long readUltrasonicCm() {
  pinMode(ULTRASONIC_PIN, OUTPUT);
  digitalWrite(ULTRASONIC_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(ULTRASONIC_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(ULTRASONIC_PIN, LOW);

  pinMode(ULTRASONIC_PIN, INPUT);
  long duration = pulseIn(ULTRASONIC_PIN, HIGH, 30000);  // 30ms timeout

  if (duration == 0) return -1;
  return duration / 29 / 2;  // convert to cm
}

// ============================================================
//  Buzzer helpers using LEDC
// ============================================================
bool buzzerAttached = false;

void buzzerTone(int freq) {
  if (!buzzerAttached) {
    ledcAttach(BUZZER_PIN, freq, 8);
    buzzerAttached = true;
  }
  ledcWriteTone(BUZZER_PIN, freq);
  ledcWrite(BUZZER_PIN, 128);
}

void buzzerOff() {
  ledcWriteTone(BUZZER_PIN, 0);
  ledcWrite(BUZZER_PIN, 0);
}

// ============================================================
//  Setup
// ============================================================
void setup() {
  Serial.begin(115200);
  while (!Serial && millis() < 3000);

  Serial.println();
  Serial.println(F("============================================="));
  Serial.println(F("  HUZZAH32 Hardware Diagnostic"));
  Serial.println(F("  Type 'c' + Enter to stop all tests"));
  Serial.println(F("============================================="));
  Serial.println();

  // Buttons (Grove: LOW = not pressed, HIGH = pressed)
  pinMode(START_BTN_PIN, INPUT);
  pinMode(SNOOZE_BTN_PIN, INPUT);

  // Servo
  myServo.attach(SERVO_PIN);
  myServo.write(0);

  // Quick self-test sequence
  Serial.println(F("[Test] Starting self-test sequence..."));
  Serial.println();

  // 1. Buzzer test
  Serial.print(F("[Buzzer] Short beep... "));
  buzzerTone(2000);
  delay(300);
  buzzerOff();
  Serial.println(F("done. Did you hear it?"));

  // 2. Servo test
  Serial.print(F("[Servo] Sweep 0 -> 90 -> 0... "));
  myServo.write(90);
  delay(600);
  myServo.write(0);
  delay(600);
  Serial.println(F("done. Did it move?"));

  // 3. Ultrasonic test
  Serial.print(F("[Ultrasonic] Reading... "));
  long dist = readUltrasonicCm();
  if (dist < 0) {
    Serial.println(F("NO ECHO (check wiring: yellow to pin 33, red to 3V, black to GND)"));
  } else {
    Serial.print(dist);
    Serial.println(F(" cm"));
  }

  // 4. Button test
  Serial.print(F("[Start Button] Current state: "));
  Serial.println(digitalRead(START_BTN_PIN) == HIGH ? "PRESSED" : "not pressed");
  Serial.print(F("[Snooze Button] Current state: "));
  Serial.println(digitalRead(SNOOZE_BTN_PIN) == HIGH ? "PRESSED" : "not pressed");

  Serial.println();
  Serial.println(F("============================================="));
  Serial.println(F("  Self-test complete. Entering live monitor."));
  Serial.println(F("  Press buttons, move hand near sensor,"));
  Serial.println(F("  and watch the output below."));
  Serial.println(F("  Type 'c' to stop."));
  Serial.println(F("============================================="));
  Serial.println();
}

// ============================================================
//  Main loop -- continuous monitoring
// ============================================================
void loop() {
  // Check for stop command
  if (Serial.available()) {
    char c = Serial.read();
    if (c == 'c' || c == 'C') {
      running = !running;
      if (!running) {
        buzzerOff();
        myServo.write(0);
        Serial.println();
        Serial.println(F(">>> STOPPED. Type 'c' to resume. <<<"));
      } else {
        Serial.println(F(">>> RESUMED <<<"));
        Serial.println();
      }
    }
  }

  if (!running) return;

  // Read all inputs
  bool startPressed  = (digitalRead(START_BTN_PIN)  == HIGH);
  bool snoozePressed = (digitalRead(SNOOZE_BTN_PIN) == HIGH);
  long distance      = readUltrasonicCm();

  // React to buttons immediately
  if (startPressed) {
    Serial.println(F("  >>> START BUTTON PRESSED -- servo sweep"));
    myServo.write(90);
    delay(500);
    myServo.write(0);
    delay(500);
  }

  if (snoozePressed) {
    Serial.println(F("  >>> SNOOZE BUTTON PRESSED -- buzzer beep"));
    buzzerTone(1500);
    delay(300);
    buzzerOff();
    delay(200);
  }

  // Periodic status print
  if (millis() - lastPrint >= PRINT_INTERVAL) {
    lastPrint = millis();

    Serial.print(F("Dist: "));
    if (distance < 0) {
      Serial.print(F("---"));
    } else {
      if (distance < 100) Serial.print(' ');
      if (distance < 10)  Serial.print(' ');
      Serial.print(distance);
    }
    Serial.print(F(" cm  |  Start: "));
    Serial.print(startPressed ? "PRESSED" : "  ---  ");
    Serial.print(F("  |  Snooze: "));
    Serial.print(snoozePressed ? "PRESSED" : "  ---  ");

    // Distance warning zone (like pill detection)
    if (distance > 0 && distance < 5) {
      Serial.print(F("  |  ** OBJECT VERY CLOSE **"));
    } else if (distance > 0 && distance < 15) {
      Serial.print(F("  |  * object detected *"));
    }

    Serial.println();
  }

  delay(50);
}
