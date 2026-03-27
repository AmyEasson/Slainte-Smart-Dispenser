#include "config.h"
#include "credentials.h"
#include "captive_portal.h"
#include "dispenser.h"
#include "network.h"
#include <WiFi.h>

enum AppState { STATE_CAPTIVE_PORTAL, STATE_CONNECTING, STATE_NORMAL };

static AppState appState;
static unsigned long lastPollTime = 0;
static unsigned long resetBtnStart = 0;
static bool resetBtnActive = false;

static void handleFactoryReset() {
  if (digitalRead(SNOOZE_BTN_PIN) != BTN_PRESSED) {
    resetBtnActive = false;
    return;
  }
  if (!resetBtnActive) {
    resetBtnActive = true;
    resetBtnStart = millis();
    Serial.println(F("[Reset] Hold 10s"));
  }

  unsigned long releaseCount = 0;
  while (true) {
    bool pressed = (digitalRead(SNOOZE_BTN_PIN) == BTN_PRESSED);
    if (!pressed) {
      releaseCount++;
      if (releaseCount * 50 >= 500) {
        resetBtnActive = false;
        return;
      }
    } else {
      releaseCount = 0;
    }

    unsigned long held = millis() - resetBtnStart;
    if (held >= FACTORY_RESET_HOLD_MS) {
      clearCredentials();
      delay(500);
      ESP.restart();
    }

    if (held >= FACTORY_RESET_WARN_MS && (held % 1000) < 60) {
      Serial.print((FACTORY_RESET_HOLD_MS - held) / 1000);
      Serial.println(F("s..."));
    }

    delay(50);
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println();
  Serial.println(F("Slainte v3.0"));
  setupHardwarePins();

  if (hasStoredCredentials()) {
    DeviceConfig cfg = loadCredentials();
    Serial.print(F("Connecting to "));
    Serial.println(cfg.ssid);
    appState = STATE_CONNECTING;
  } else {
    Serial.println(F("Setup: Connect to Slainte_Setup WiFi, open 192.168.4.1"));
    startCaptivePortal();
    appState = STATE_CAPTIVE_PORTAL;
  }
}

void loop() {
  switch (appState) {
    case STATE_CAPTIVE_PORTAL:
      handleCaptivePortal();
      if (portalCredentialsReceived()) {
        delay(1000);
        ESP.restart();
      }
      break;

    case STATE_CONNECTING: {
      DeviceConfig cfg = loadCredentials();
      if (connectToWiFi(cfg.ssid, cfg.password, WIFI_CONNECT_RETRIES)) {
        configureServer(cfg.serverAddr, cfg.serverPort);
        Serial.print(F("Ready "));
        Serial.println(WiFi.localIP());
        Serial.println(F("Hold Snooze 10s to reset WiFi"));
        appState = STATE_NORMAL;
        lastPollTime = millis();
      } else {
        Serial.println(F("WiFi failed"));
        clearCredentials();
        delay(500);
        ESP.restart();
      }
      break;
    }

    case STATE_NORMAL:
      if (digitalRead(START_BTN_PIN) == BTN_PRESSED) {
        Serial.println(F("[Btn] Start"));
        runDispenseCycle();
        delay(1000);
      }
      if (millis() - lastPollTime >= POLL_INTERVAL) {
   
