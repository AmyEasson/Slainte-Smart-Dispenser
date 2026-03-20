#include "network.h"
#include "config.h"
#include "dispenser.h"
#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

static char serverAddress[81];
static uint16_t serverPort;

static int  lastDispenseIntakeIds[MAX_INTAKE_IDS];
static int  lastDispenseIntakeIdsCount = 0;

static String buildUrl(const char* path) {
  return String("http://") + serverAddress + ":" + String(serverPort) + path;
}

bool connectToWiFi(const char* ssid, const char* password, int maxRetries) {
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  delay(500);

  for (int i = 1; i <= maxRetries; i++) {
    WiFi.begin(ssid, password);

    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - start < 8000) {
      delay(250);
    }

    if (WiFi.status() == WL_CONNECTED)
      return true;

    WiFi.disconnect();
    delay(2000);
  }

  return false;
}

void configureServer(const char* addr, uint16_t port) {
  strncpy(serverAddress, addr, sizeof(serverAddress) - 1);
  serverPort = port;
}

void checkServerForCommands() {
  HTTPClient http;
  String url = buildUrl("/api/firmware/poll?deviceId=DISPENSER_001");

  http.begin(url);
  http.setTimeout(5000);
  int code = http.GET();

  if (code != 200) {
    http.end();
    return;
  }

  String body = http.getString();
  http.end();

  StaticJsonDocument<256> doc;
  if (deserializeJson(doc, body)) return;

  const char* cmd = doc["command"];
  if (!cmd) return;

  if (strcmp(cmd, "DISPENSE") == 0) {
    lastDispenseIntakeIdsCount = 0;
    JsonArray ids = doc["intakeIds"];
    if (!ids.isNull()) {
      for (size_t i = 0; i < ids.size() && i < (size_t)MAX_INTAKE_IDS; i++)
        lastDispenseIntakeIds[lastDispenseIntakeIdsCount++] = (int)ids[i];
    }
    runDispenseCycle();
  }

  if (strcmp(cmd, "ADVANCE") == 0) {
    Serial.println(F("[Net] Advance command received"));
    runAdvanceCycle();
  }
}

bool checkForSnoozeCommand() {
  HTTPClient http;
  String url = buildUrl("/api/firmware/poll?deviceId=DISPENSER_001");

  http.begin(url);
  http.setTimeout(3000);
  int code = http.GET();

  if (code != 200) {
    http.end();
    return false;
  }

  String body = http.getString();
  http.end();

  StaticJsonDocument<128> doc;
  if (deserializeJson(doc, body)) return false;

  const char* cmd = doc["command"];
  return (cmd && strcmp(cmd, "SNOOZE") == 0);
}

void sendStatusReport(bool wasTaken) {
  if (lastDispenseIntakeIdsCount == 0) return;

  HTTPClient http;
  String url = buildUrl("/api/firmware/status?deviceId=DISPENSER_001");

  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(5000);

  StaticJsonDocument<256> doc;
  JsonArray ids = doc.createNestedArray("intakeIds");
  for (int i = 0; i < lastDispenseIntakeIdsCount; i++)
    ids.add(lastDispenseIntakeIds[i]);
  doc["dispenseEventStatus"] = wasTaken;

  String json;
  serializeJson(doc, json);

  http.POST(json);
  http.end();

  lastDispenseIntakeIdsCount = 0;
}
