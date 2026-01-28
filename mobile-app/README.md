# Mobile App

This directory contains the mobile application code.

Include here:
- App source code
- UI components
- State management
- Platform-specific configurations

Add setup and run instructions once the stack is finalised.

## Vitamin intake logging feature

Requires a GET API to retrieve existing intake logs.

Expected JSON structure:
```json
[
  {
    "id": "number | string",
    "vitamin": "string",
    "scheduledTime": "ISO-8601 timestamp",
    "status": "TAKEN | PENDING | MISSED",
    "takenAt": "ISO-8601 timestamp | null"
  }
]
```

## User intake scheduling tool

Requires a GET API to retrieve the existing schedule.
Requires a POST (or PUT) API to update the existing schedule.

Expected json structure:
```json
{
  "vitamins": [
    {
      "vitaminType": "string",
      "schedule": [
        {
          "day": "MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY",
          "times": ["HH:MM", "HH:MM"]
        }
      ]
    }
  ]
}
```
Each entry in schedule represents one intake slot.
Multiple entries with the same day and times are allowed, meaning the same vitamin needs to be taken multiple times within the same time window.




