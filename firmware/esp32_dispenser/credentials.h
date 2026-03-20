#ifndef CREDENTIALS_H
#define CREDENTIALS_H

#include <stdint.h>

struct DeviceConfig {
  char     ssid[33];
  char     password[64];
  char     serverAddr[81];
  uint16_t serverPort;
};

bool         hasStoredCredentials();
DeviceConfig loadCredentials();
void         saveCredentials(const DeviceConfig& cfg);
void         clearCredentials();

#endif
