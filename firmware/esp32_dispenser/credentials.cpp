#include "credentials.h"
#include "config.h"
#include <Preferences.h>
#include <string.h>

static Preferences prefs;

bool hasStoredCredentials() {
  prefs.begin(PREFS_NAMESPACE, true);
  bool valid = prefs.getBool(PREFS_KEY_VALID, false);
  prefs.end();
  return valid;
}

DeviceConfig loadCredentials() {
  DeviceConfig cfg;
  memset(&cfg, 0, sizeof(cfg));

  prefs.begin(PREFS_NAMESPACE, true);
  String s = prefs.getString(PREFS_KEY_SSID, "");
  String p = prefs.getString(PREFS_KEY_PASS, "");
  String sv = prefs.getString(PREFS_KEY_SERVER, "");
  cfg.serverPort = prefs.getUShort(PREFS_KEY_PORT, DEFAULT_SERVER_PORT);
  prefs.end();

  strncpy(cfg.ssid, s.c_str(), sizeof(cfg.ssid) - 1);
  strncpy(cfg.password, p.c_str(), sizeof(cfg.password) - 1);
  strncpy(cfg.serverAddr, sv.c_str(), sizeof(cfg.serverAddr) - 1);

  return cfg;
}

void saveCredentials(const DeviceConfig& cfg) {
  prefs.begin(PREFS_NAMESPACE, false);
  prefs.putString(PREFS_KEY_SSID,   cfg.ssid);
  prefs.putString(PREFS_KEY_PASS,   cfg.password);
  prefs.putString(PREFS_KEY_SERVER, cfg.serverAddr);
  prefs.putUShort(PREFS_KEY_PORT,   cfg.serverPort);
  prefs.putBool(PREFS_KEY_VALID,    true);
  prefs.end();
}

void clearCredentials() {
  prefs.begin(PREFS_NAMESPACE, false);
  prefs.clear();
  prefs.end();
}
