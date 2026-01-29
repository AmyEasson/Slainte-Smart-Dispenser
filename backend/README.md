# Overview

This backend supports the smart vitamin dispenser with two clients:
- Firmware (embedded device - Arduino)
- Mobile App (user-facing)
The system manages vitamin schedules, dispensing events, and intake history.

## High-Level Architecture
Mobile App ──► ScheduleService ──► ScheduleRepository
│
Firmware ──► FirmwareScheduleService ──► IntakeAttemptRepository
│
Firmware ──► IntakeRecordingService ──► IntakeAttemptRepository
│
Mobile App ──► IntakeHistoryService ──► IntakeAttemptRepository


## Core Concepts
### Schedule
Created by the mobile app. Defines:
- vitamin type
- number of pills
- days and times
Stored as DispenseSchedule

### IntakeAttempt
Represents one scheduled intake event.
Tracks:
- intake ID
- scheduled time
- status (PENDING, TAKEN, MISSED)
- reported time
This is the only persistent state in the system.

## Services
### FirmwareScheduleService
Purpose: Provides firmware with intake events that are due.
Used by: Firmware
Type: Read + create pending attempts

### IntakeRecordingService
Purpose: Records whether a dispense was successful or missed.
Used by: Firmware
Type: Write-only

### IntakeHistoryService
Purpose: Provides intake history to mobile app.
Used by: Mobile app
Type: Read-only

## Data Flow
1. Mobile app submits schedule
2. Firmware requests due intakes
3. Backend creates IntakeAttempt entries
4. Firmware reports success/failure
5. Mobile app queries intake history

## Notes
- no database is currently used
- IntakeAttemptRepository is in-memory
- DTOs are API-only
