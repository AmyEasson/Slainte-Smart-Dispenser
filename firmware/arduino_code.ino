#include <Servo.h>

const int IR_SENSOR = 0; // Pin A0
const int MOTOR = 9; // Pin ~9
const int BUZZER = 8; // Pin 8
const int SIMULATE_START = 2; // Pin 2
const int SIMULATE_SNOOZE = 3; // Pin ~3


// 1. Wait after dispensing before checking sensor
const unsigned long TIME_WAIT_AFTER_DISPENSE = 3000; // Sim: 3 sec | Real: 300000 (5 mins)
// 2. How long to read the sensor to confirm pill presence
const unsigned long TIME_READ_SENSOR = 2000;         // Sim: 2 sec | Real: 15000 (15 sec)
// 3. How long to ring the alarm
const unsigned long TIME_ALARM_DURATION = 5000;      // Sim: 5 sec | Real: 600000 (10 mins)
// 4. Snooze duration
const unsigned long TIME_SNOOZE_DURATION = 3000;     // Sim: 3 sec | Real: 300000 (5 mins)
//5. Degrees to rotate the servo
const int DEGREES = 150;
// --- THRESHOLDS ---
const int SENSOR_THRESHOLD = 200; // Above this = Pill detected (adjust for your potentiometer)



// --- OBJECTS ---
Servo myServo;

void setup() {
  Serial.begin(9600);
  
  // Setup Components
  myServo.attach(MOTOR);
  myServo.write(0); // Reset servo to 0
  
  pinMode(BUZZER, OUTPUT);
  
  // INPUT_PULLUP means the pin is HIGH when open, LOW when pressed
  pinMode(SIMULATE_START, INPUT_PULLUP);
  pinMode(SIMULATE_SNOOZE, INPUT_PULLUP);
  
  Serial.println("System Ready. Press Button 1 to Dispense.");
}

void loop() {
  // 1. Check for Start Signal (Button 1)
  if (digitalRead(SIMULATE_START) == LOW) {
    runDispenseCycle();
  }
}

// --- MAIN LOGIC FLOW ---
void runDispenseCycle() {
  Serial.println("--- Cycle Started ---");
  
  // Step 1: Rotate Servo 30 degrees
  Serial.println("Dispensing Pill...");
  myServo.write(DEGREES);
  delay(1000);
  myServo.write(0); // Return to 0
  
  // Step 2: Wait 5 minutes before checking
  Serial.println("Waiting 5 minutes (Pre-Check)...");
  smartDelay(TIME_WAIT_AFTER_DISPENSE); 
  
  // Step 3: Start Monitor/Alarm Loop
  bool pillTaken = false;
  
  while (!pillTaken) {
    
    // A. Read sensor for 15 seconds
    Serial.println("Checking Sensor for 15 seconds...");
    bool pillDetected = checkSensorForTime(TIME_READ_SENSOR);
    
    if (!pillDetected) {
      Serial.println("Tray Empty. Pill taken!");
      pillTaken = true; // Exit loop
    } else {
      // B. Pill still there? Ring Buzzer!
      Serial.println("Pill Detected! Ringing Alarm...");
      
      // This function returns TRUE if user hit snooze, FALSE if time ran out
      bool snoozed = ringBuzzerWithSnooze(TIME_ALARM_DURATION);
      
      if (snoozed) {
        Serial.println("Snooze Pressed! Pausing for 5 minutes...");
        noTone(BUZZER); // Stop noise immediately
        smartDelay(TIME_SNOOZE_DURATION); // Wait 5 mins
        // Loop triggers again -> Goes back to Step A (Check Sensor)
      } else {
        Serial.println("Alarm finished (No Snooze). Stopping Buzzer.");
        noTone(BUZZER);
        break; // Stop nagging (or remove this break to buzz forever)
      }
    }
  }
  
  Serial.println("--- Cycle Finished ---");
}

// --- HELPER FUNCTIONS ---

// 1. Sensor Check Function
// Returns true if pill is seen, false if empty
bool checkSensorForTime(unsigned long duration) {
  unsigned long start = millis();
  int detections = 0;
  
  while (millis() - start < duration) {
    int val = analogRead(IR_SENSOR);
    
    // If potentiometer is "High" (Right side), we count it as a pill
    if (val > SENSOR_THRESHOLD) {
      detections++;
    }
    delay(100); // Short check interval
  }
  
  // If we saw the "pill" significantly, return true
  return (detections > 5); 
}

// 2. Alarm Function with Snooze
// Returns true if Snooze was pressed
bool ringBuzzerWithSnooze(unsigned long duration) {
  unsigned long start = millis();
  
  while (millis() - start < duration) {
    // Ring Buzzer
    tone(BUZZER, 1000); // 1KHz sound
    
    // CHECK SNOOZE BUTTON
    if (digitalRead(SIMULATE_SNOOZE) == LOW) {
      return true; // Exit immediately if pressed
    }
    
    // Check if they took the pill WHILE it was buzzing
    if (analogRead(IR_SENSOR) < SENSOR_THRESHOLD) {
       Serial.println("Pill taken during alarm!");
       return false; // Treat as finished
    }
    
    delay(50); // Small delay for stability
  }
  
  return false; // Time ran out, button was never pressed
}

// 3. Smart Delay
// Just waits, but keeps things stable
void smartDelay(unsigned long duration) {
  unsigned long start = millis();
  while (millis() - start < duration) {
    // We could check for "Cancel" buttons here if we wanted
    delay(10);
  }
}
