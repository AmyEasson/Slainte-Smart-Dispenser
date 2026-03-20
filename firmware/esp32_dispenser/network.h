#ifndef NETWORK_H
#define NETWORK_H

#include <stdbool.h>
#include <stdint.h>

bool connectToWiFi(const char* ssid, const char* password, int maxRetries);
void configureServer(const char* addr, uint16_t port);
void checkServerForCommands();
bool checkForSnoozeCommand();
void sendStatusReport(bool wasTaken);

#endif
