# Backend

This directory contains the backend service for the Smart Vitamin Dispenser.

## Tech Stack

- Java 23
- Spring Boot 4.0
- Spring Security (JWT authentication)
- Spring Data JPA
- H2 (file-based database in production, in-memory for tests)
- Maven

## Package Structure

```
com.vitamindispenser.backend
├── auth/               Registration and login
├── firmware/           Firmware polling, command handling, and status reporting
├── schedule/           Scheduling logic, slot assignment, and refill tracking
├── mobile/             Mobile app controller (schedule, slots, intake, logs)
├── logging/            Intake logging and CSV export
├── device/             Device entity and repository
├── user/               User entity and repository
├── security/           JWT filter and utility
├── config/             Security config, CORS, and data initialisation
└── exceptions/         Custom exceptions
```

## Running the Backend

1. Copy `application-local.properties.example` to `application-local.properties` and fill in your values (this file is gitignored — do not commit it)
2. Run via IntelliJ or `mvn spring-boot:run`
3. Server starts on `http://localhost:8080`

## Authentication

The backend uses JWT (JSON Web Token) based authentication. Most endpoints require a valid token.

### How it works

1. Register a user account via `POST /api/auth/register`
2. Log in via `POST /api/auth/login` — you will receive a token in the response
3. Include the token in the `Authorization` header of every subsequent request:

```
Authorization: Bearer <your token here>
```

Tokens expire after 24 hours, after which you need to log in again.

### Exceptions (no token required)

- `POST /api/auth/register`
- `POST /api/auth/login`
- All `/api/firmware/**` endpoints (the physical device cannot authenticate)

---

## API Endpoints

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/auth/register | None | Register a new user account |
| POST | /api/auth/login | None | Log in and receive a JWT token |

**Register / Login request body:**

```json
{
  "username": "yourname",
  "password": "yourpassword"
}
```

**Login response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Mobile App — `/api/mobile`

All endpoints require a valid JWT token.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/mobile/claim-device | Required | Links the physical dispenser to the logged-in user |
| POST | /api/mobile/schedule | Required | Save or overwrite the vitamin schedule for the logged-in user |
| GET | /api/mobile/getSchedule | Required | Retrieve the current schedule for the logged-in user |
| GET | /api/mobile/slots | Required | Get all 15 slots with assigned vitamins |
| GET | /api/mobile/slots/refill-info | Required | Get refill date, days until refill, and any warnings |
| POST | /api/mobile/slots/confirm-fill | Required | Confirm dispenser has been filled, advances fill cycle |
| GET | /api/mobile/intake | Required | Get raw vitamin intake data for the dashboard |
| GET | /api/mobile/logs/export.csv | Required | Download full intake log history as a CSV file |

**Claim device request body:**

```json
{
  "deviceId": "DISPENSER_001"
}
```

**Schedule request/response body:**

```json
{
  "vitamins": [
    {
      "vitaminType": "Vitamin C",
      "numberOfPills": 1,
      "schedule": [
        {"day": "monday", "times": ["09:00", "21:00"]},
        {"day": "friday", "times": ["09:00"]}
      ]
    }
  ]
}
```

**Slots response:**

```json
{
  "slots": [
    {
      "slotNumber": 1,
      "reserved": false,
      "assignedDay": "monday",
      "assignedTime": "09:00",
      "vitamins": [
        {"vitaminType": "Vitamin C", "numberOfPills": 1}
      ]
    },
    {
      "slotNumber": 15,
      "reserved": true,
      "assignedDay": null,
      "assignedTime": null,
      "vitamins": []
    }
  ]
}
```

**Refill info response:**

```json
{
  "lastFillDate": "2026-03-12",
  "refillDate": "2026-04-09",
  "daysUntilRefill": 28,
  "warning": null
}
```

Possible `warning` values: `null`, `"NO_SCHEDULE"`, `"SCHEDULE_CHANGED"`.

---

### Firmware — `/api/firmware`

These endpoints are called by the physical dispenser. No authentication required.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/firmware/poll?deviceId=DISPENSER_001 | None | Dispenser polls this to get its next command |
| POST | /api/firmware/command | None | Mobile app sets a one-shot command (SNOOZE only) |
| POST | /api/firmware/status | None | Dispenser reports whether vitamins were taken after dispensing |

**Poll response:**

```json
{
  "command": "IDLE",
  "intakeIds": [],
  "slotNumber": null
}
```

Commands: `IDLE`, `DISPENSE`, `SNOOZE`. When `DISPENSE`, `intakeIds` contains the IDs of the schedule entries being dispensed and `slotNumber` is the physical slot to rotate to — both must be sent back in the status report.

**Status request body:**

```json
{
  "intakeIds": [1, 2, 3],
  "dispenseEventStatus": true
}
```

**Command request body** (SNOOZE only — DISPENSE is controlled by the schedule):

```json
{
  "command": "SNOOZE"
}
```

---

## Data Model

- **User** — a registered account. Owns a device and a schedule. Tracks `lastFillDate`, `scheduleChanged`, and `fillCycleOffset` for the slot refill cycle.
- **Device** — represents a physical dispenser. Linked to a user via `claim-device`.
- **ScheduleEntry** — a single dispense event (one vitamin, one day, one time). Many entries belong to one user.
- **Slot** — one of 15 physical slots in the dispenser. Slot 15 is reserved as the dispense opening and is never assigned. Slots 1–14 are assigned a (day, time) pair from the user's schedule, repeating the pattern cyclically to fill all 14 slots. The fill cycle offset advances by 14 on each confirmed refill, allowing schedules with more than 14 unique times to be cycled through over multiple fills.

---

## Slot System

The dispenser has 15 physical slots arranged in a wheel. Slot 15 is always positioned at the dispense opening and is reserved — it is never loaded with vitamins.

When a schedule is saved, the backend automatically assigns each of the 14 remaining slots a (day, time) pair from the schedule. If the schedule has fewer than 14 unique times, the pattern repeats to fill all slots. If it has more than 14, the first 14 (starting from the current time of day) are assigned, and subsequent fills cycle through the remainder.

The user is shown a refill date based on when slot 14's assigned time will next occur. When they confirm a refill via the app, the fill cycle advances.

---

## Configuration

The following properties must be set in your local `application-local.properties` (gitignored):

```
jwt.secret=your-secret-key-here
```

The active profile is set in `application.properties`:

```
spring.profiles.active=local
```

Test profile uses an in-memory H2 database and a dummy JWT secret — see `application-test.properties`.