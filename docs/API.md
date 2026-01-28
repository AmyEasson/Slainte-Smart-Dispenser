## API documentation

This document describes the backend API.

Include here:
- Base URL(s)
- Authentication method
- Available endpoints
- Request/response examples
- Error formats

This should reflect the current backend implementation.


**Firmware Controller:**

_Development design decision:_ use of mock databases to enable parallel development of system components.
For reporting vitamin consumption status from the Arduino, the controller endpoint /status is used. The Arduino must provide both a vitamin batch ID and a status value. The batch ID is then used to retrieve details about the vitamins being consumed, including the vitamin type, number of pills, and the scheduled day and time.
This additional information is stored in the schedule database, which in our implementation is a CSV file. However, since the database is currently empty and the firmware is still under development, a temporary data source is required. To support ongoing firmware development, a mock schedule database is therefore being created.
See src/main/resources/mock/schedule.csv
To help with setting up the database, even when it's mock, I'm adding a Maven dependency: commons-csv