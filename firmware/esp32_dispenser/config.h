#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>

#define ULTRASONIC_PIN   33
#define SERVO_PIN        27
#define BUZZER_PIN       21
#define START_BTN_PIN    15
#define SNOOZE_BTN_PIN   14
#define BTN_PRESSED      HIGH

#define TIME_WAIT_AFTER_DISPENSE  3000
#define TIME_READ_SENSOR          2000
#define TIME_ALARM_DURATION       5000
#define TIME_SNOOZE_DURATION      3000
#define POLL_INTERVAL             2000
#define FACTORY_RESET_HOLD_MS     10000
#define FACTORY_RESET_WARN_MS     3000

#define SERVO_DISPENSE_DEGREES    98
#define ULTRASONIC_THRESHOLD_CM   8
#define MAX_INTAKE_IDS            10
#define WIFI_CONNECT_RETRIES      20

#define PREFS_NAMESPACE  "slainte"
#define PREFS_KEY_SSID   "ssid"
#define PREFS_KEY_PASS   "pass"
#define PREFS_KEY_SERVER "server"
#define PREFS_KEY_PORT   "port"
#define PREFS_KEY_VALID  "valid"

#define AP_SSID "Slainte_Setup"
#define AP_WEB_PORT 80
#define DEFAULT_SERVER_PORT 8080

#endif
