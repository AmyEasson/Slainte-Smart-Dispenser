# Slainte — Vitamin Dispenser

An automated vitamin dispenser system with a mobile app, Spring Boot backend, and ESP32 firmware.

## Repository Structure

```
docs/         Project documentation (setup, architecture, API)
firmware/     ESP32 Arduino firmware for the dispenser hardware
backend/      Spring Boot REST API
app/          React Native (Expo) mobile application
hardware/     CAD files, schematics, and hardware documentation
```

---

## Prerequisites

Make sure you have the following installed before getting started:

- **Java 17+** (for the backend)
- **Maven** (for the backend)
- **Node.js 18+** and **npm** (for the mobile app)
- **Expo CLI** — `npm install -g expo-cli`
- **Arduino IDE 2.x** (for the firmware)

---

## Running the Backend

```bash
cd backend
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Database

The backend uses a **persistent H2** database. It is created automatically on first run — you do not need to set anything up manually.

> **If you get `Unknown device` or `User does not exist` errors**, your database is likely stale from a schema change. Delete the H2 database file (usually `*.db` or `*.mv.db` in the project root or `backend/` directory) and restart the backend — it will recreate all tables fresh. You will need to re-register any test devices and users after doing this.

### Registering the Test Device

The firmware uses device ID `DISPENSER_001`. Make sure this device is registered in the database and linked to a user with an active schedule, otherwise the poll endpoint will always return `IDLE`.

### Testing the API (Postman)

Poll for a dispense command:
```
GET http://localhost:8080/api/firmware/poll?deviceId=DISPENSER_001
```

Report dispense status:
```
POST http://localhost:8080/api/firmware/status?deviceId=DISPENSER_001
Content-Type: application/json

{
  "intakeIds": [1, 2, 3],
  "dispenseEventStatus": true
}
```
Set `dispenseEventStatus` to `false` to simulate a missed dose.

---

## Running the Mobile App

```bash
cd app
npm install
npx expo start
```

Scan the QR code with the **Expo Go** app on your phone, or press `i` for iOS simulator / `a` for Android emulator.

---

## Setting Up the Firmware

### 1. Install Arduino IDE 2.x

Download from [arduino.cc](https://www.arduino.cc/en/software).

### 2. Add the ESP32 Board Package

In Arduino IDE go to **File → Preferences** (or **Arduino IDE → Settings** on Mac) and add this URL to "Additional boards manager URLs":

```
https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```

Then go to **Tools → Board → Boards Manager**, search for `esp32` by Espressif Systems, and install version **2.0.17**.

> **Do not install version 3.x** — the firmware uses `ledcSetup`/`ledcAttachPin` APIs that were removed in 3.x and it will not compile.

### 3. Select the Board

**Tools → Board → esp32 → Adafruit ESP32 Feather**

### 4. Install Required Libraries

Go to **Tools → Manage Libraries** and install:

- `ESP32Servo` by Kevin Harrington
- `ArduinoJson` by Benoit Blanchon

`WiFi`, `WebServer`, and `HTTPClient` are built into the ESP32 package — no separate install needed.

> If you have **WiFiNINA** installed from a previous Arduino project, uninstall it — it conflicts with the ESP32 WiFi library.

### 5. Upload the Firmware

Open `firmware/esp32_dispenser/esp32_dispenser.ino` in Arduino IDE, select the correct port under **Tools → Port**, and click Upload.

A successful upload ends with:
```
Hash of data verified.
Leaving...
Hard resetting via RTS pin...
```

---

## Connecting Everything to the Same Network

For local testing, all three components (laptop running backend, phone running app, Arduino) need to be on the **same network**.

The easiest approach is to use your **phone's hotspot**:

1. Turn on your phone's Personal Hotspot
2. Connect your **laptop** to the hotspot
3. Find your laptop's IP on the hotspot network:
   ```bash
   ipconfig getifaddr en0
   ```
4. Make sure your Spring backend is running before the Arduino connects

> **Rename your hotspot** to something with no special characters before using it with the Arduino — apostrophes can cause the ESP32 WiFi connection to silently fail. 

---

## First-Time Device WiFi Setup (Captive Portal)

On first boot (or after a factory reset), the Arduino creates its own WiFi hotspot called `Slainte_Setup`.

1. Connect your phone to `Slainte_Setup`
2. Open a browser and go to `http://192.168.4.1`
3. Enter your WiFi network name and password, your laptop's IP address, and port `8080`
4. Tap Save — the device will reboot and connect

To factory reset the device and re-run setup, hold the **Snooze button for 10 seconds**.

### Serial Monitor

To see what the device is doing, open **Tools → Serial Monitor** in Arduino IDE (separate from the upload window) and set the baud rate to **115200**. A successful boot and connection looks like:

```
Slainte v3.0
Connecting to YourNetwork
Ready 172.20.10.x
Hold Snooze 10s to reset WiFi
```

If you see it repeatedly printing the bootloader lines and restarting, check that nothing is pulling too much current from the board on startup (particularly the servo).

---

## Firmware API Compatibility Notes

The firmware communicates with the backend using these endpoints:

| Action | Method | Endpoint |
|--------|--------|----------|
| Poll for command | GET | `/api/firmware/poll?deviceId=DISPENSER_001` |
| Report dispense status | POST | `/api/firmware/status?deviceId=DISPENSER_001` |
| Set snooze command | POST | `/api/firmware/command` |

The poll response must be in this format:
```json
{
  "command": "IDLE | DISPENSE | SNOOZE | ADVANCE",
  "intakeIds": [1, 2, 3],
  "slotNumber": 1
}
```
