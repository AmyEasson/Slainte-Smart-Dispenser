# Sláinte — Smart Vitamin Dispenser

An automated vitamin dispensing system that combines **hardware, a mobile app, and a backend service** to help users stay consistent with their medication and supplement routines.

---

## Overview

Sláinte is designed to reduce missed doses and simplify daily routines by:

- Automatically dispensing scheduled vitamins
- Tracking adherence and missed doses
- Allowing remote schedule management via a mobile app
- Integrating embedded hardware with a backend system

The project brings together **embedded systems, full-stack development, and real-world product design**.

---

## Features

- Scheduled vitamin dispensing  
- Mobile app for managing routines  
- Snooze and missed-dose handling  
- Real-time communication between device and backend  
- Intake tracking and logging  

---

## System Architecture

```
[ Mobile App ]  ⇄  [ Spring Boot Backend ]  ⇄  [ ESP32 Dispenser ]
```

- **Mobile App (React Native / Expo)**  
  User interface for creating schedules and tracking intake  

- **Backend (Spring Boot)**  
  Handles scheduling logic, user management, and device communication  

- **Firmware (ESP32)**  
  Controls the physical dispenser and communicates with the backend over WiFi  

---

## Repository Structure

```
docs/         Documentation (setup, API, architecture)
firmware/     ESP32 firmware
backend/      Spring Boot API
app/          React Native mobile app
hardware/     CAD + electronics design
```

---

## Getting Started

To run the full system locally:

```bash
# Backend
cd backend
mvn spring-boot:run

# Mobile app
cd app
npm install
npx expo start
```

The firmware can be uploaded to an ESP32 board using the Arduino IDE.

Full setup instructions: see `docs/SETUP.md`  
API documentation: see `docs/API.md`  

---

## Example Use Case

1. A user creates a vitamin schedule in the app  
2. The backend stores and manages the schedule  
3. The dispenser periodically polls the backend  
4. When it’s time, the device dispenses the correct vitamins  
5. The event is logged and visible in the app  

---

## Hardware

The dispenser is built using:

- ESP32 microcontroller  
- Servo motor for dispensing  
- Custom housing
- Grove Ultrasonic sensor for intake tracking 

---

## Project Status

This project was developed as part of a university project and is actively being refined.

---

## Future Improvements

- Cloud deployment  
- Improved hardware reliability  
- Multi-device support  
