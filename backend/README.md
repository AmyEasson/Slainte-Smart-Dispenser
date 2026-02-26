# Backend

This directory contains the backend service for the Smart Vitamin Dispenser.

## Tech Stack
- Java 23
- Spring Boot 4.0
- Spring Security (JWT authentication)
- Spring Data JPA
- H2 (in-memory database)
- Maven

## Package Structure

```
com.vitamindispenser.backend
├── controllers/        REST controllers (HTTP layer only)
├── domain/             Business logic / services
├── model/              JPA entities (User, Device, ScheduleEntry)
├── repository/         Data access interfaces
├── security/           JWT filter, utility, and security config
├── dto/                Request/response body classes
└── resources/          Configuration files
```

## Running the Backend

1. Copy `application-local.properties.example` to `application-local.properties` and fill in your values (this file is gitignored — do not commit it)
2. Run via IntelliJ or `mvn spring-boot:run`
3. Server starts on `http://localhost:8080`

## Authentication

The backend uses **JWT (JSON Web Token)** based authentication. Most endpoints require a valid token.

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
| POST | `/api/auth/register` | None | Register a new user account |
| POST | `/api/auth/login` | None | Log in and receive a JWT token |

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
| POST | `/api/mobile/claim-device` | Required | Links the physical dispenser to the logged-in user |
| POST | `/api/mobile/schedule` | Required | Save or overwrite the vitamin schedule for the logged-in user |
| GET | `/api/mobile/getSchedule` | Required | Retrieve the current schedule for the logged-in user |
| GET | `/api/mobile/intake` | Required | Get raw vitamin intake data for the dashboard |
| GET | `/api/mobile/logs/export.csv` | Required | Download full intake log history as a CSV file |

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

---

### Firmware — `/api/firmware`

These endpoints are called by the physical dispenser. No authentication required.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/firmware/poll?deviceId=DISPENSER_001` | None | Dispenser polls this to get its next command |
| POST | `/api/firmware/command` | None | Mobile app sets a one-shot command (SNOOZE only) |
| POST | `/api/firmware/status` | None | Dispenser reports whether vitamins were taken after dispensing |
| GET | `/api/firmware/schedule` | None | Alternative schedule fetch for firmware (not used by current firmware) |

**Poll response:**
```json
{
  "command": "IDLE",
  "intakeIds": []
}
```
Commands: `IDLE`, `DISPENSE`, `SNOOZE`. When `DISPENSE`, `intakeIds` contains the IDs of the schedule entries being dispensed — these must be sent back in the status report.

**Status request body:**
```json
{
  "intakeIds": [1, 2, 3],
  "dispenseEventStatus": true
}
```

**Command request body (SNOOZE only — DISPENSE is controlled by the schedule):**
```json
{
  "command": "SNOOZE"
}
```

---

## Data Model

- **User** — a registered account. Owns a device and a schedule.
- **Device** — represents a physical dispenser. Linked to a user via `claim-device`.
- **ScheduleEntry** — a single dispense event (one vitamin, one day, one time). Many entries belong to one user.

In the current demo setup there is one physical device with ID `DISPENSER_001`. A user must claim it before polling will return their schedule.

---

## Configuration

The following properties must be set in your local `application-local.properties` (gitignored):

```properties
jwt.secret=your-secret-key-here
```

The active profile is set in `application.properties`:
```properties
spring.profiles.active=local
```