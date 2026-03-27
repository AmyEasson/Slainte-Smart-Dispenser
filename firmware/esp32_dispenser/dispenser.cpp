#include "dispenser.h"
#include "config.h"
#include "network.h"
#include <Arduino.h>
#include <ESP32Servo.h>

static Servo dispenserServo;
static const int BUZZER_CHANNEL = 4;
static bool  buzzerAttached = false;

static void buzzerTone(int freq) {
  if (!buzzerAttached) {
    ledcSetup(BUZZER_CHANNEL, 2000, 8);
    ledcAttachPin(BUZZER_PIN, BUZZER_CHANNEL);
    buzzerAttached = true;
  }
  ledcWriteTone(BUZZER_CHANNEL, freq);
}

static void buzzerOff() {
  ledcWriteTone(BUZZER_CHANNEL, 0);
  ledcWrite(BUZZER_CHANNEL, 0);
}

static long readUltrasonicCm() {
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

static bool checkSensorForTime(unsigned long durationMs) {
  unsigned long start = millis();
  while (millis() - start < durationMs) {
    long cm = readUltrasonicCm();
    Serial.print(F("[Sensor] "));
    if (cm < 0) Serial.println(F("err")); else { Serial.print(cm); Serial.println(F(" cm")); }
    if (cm > 0 && cm < ULTRASONIC_THRESHOLD_CM)
      return true;
    delay(200);
  }
  return false;
}

static bool ringBuzzerHybrid(unsigned long durationMs) {
  unsigned long start = millis();
  unsigned long lastSnoozeCheck = 0;

  buzzerTone(2000);

  while (millis() - start < durationMs) {
    if (digitalRead(SNOOZE_BTN_PIN) == BTN_PRESSED) {
      buzzerOff();
      Serial.println(F("[Btn] Snooze"));
      return true;
    }
    if (millis() - lastSnoozeCheck > 1000) {
      lastSnoozeCheck = millis();
      if (checkForSnoozeCommand()) {
        buzzerOff();
        return true;
      }
    }
    delay(50);
  }

  buzzerOff();
  return false;
}

void setupHardwarePins() {
  dispenserServo.attach(SERVO_PIN);
  dispenserServo.write(0);

  pinMode(START_BTN_PIN, INPUT);
  pinMode(SNOOZE_BTN_PIN, INPUT);

  buzzerTone(2000);
  delay(100);
  buzzerOff();
}

void runDispenseCycle() {
  Serial.println(F("[Dispenser] Entering dispense cycle..."));
  dispenserServo.write(SERVO_DISPENSE_DEGREES);
  delay(300);
  dispenserServo.write(0);

  delay(TIME_WAIT_AFTER_DISPENSE);

  bool pillTaken = false;
  while (!pillTaken) {
    bool pillDetected = checkSensorForTime(TIME_READ_SENSOR);

    if (!pillDetected) {
      sendStatusReport(true);
      pillTaken = true;
    } else {
      bool snoozed = ringBuzzerHybrid(TIME_ALARM_DURATION);
      if (snoozed) {
        delay(TIME_SNOOZE_DURATION);
      } else {
        sendStatusReport(false);
        break;
      }
    }
  }
}

void runAdvanceCycle() {
  Serial.println(F("[Dispenser] Advancing carousel..."));
  dispenserServo.write(SERVO_DISPENSE_DEGREES);
  delay(1000);
  dispenserServo.write(0);
  delay(TIME_WAIT_AFTER_DISPENSE);
}
