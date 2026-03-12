package com.vitamindispenser.backend.firmware;

import com.vitamindispenser.backend.logging.LoggingService;
import com.vitamindispenser.backend.firmware.dto.IntakeReport;
import com.vitamindispenser.backend.device.Device;
import com.vitamindispenser.backend.schedule.ScheduleEntry;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/firmware")
public class FirmwareController {
    /*
     * Firmware workflow:
     * 1. Firmware polls /poll to get command (IDLE | DISPENSE | SNOOZE) based on schedule
     * 2. Firmware dispenses when it receives DISPENSE
     * 3. Firmware sends boolean (vitamin taken yes/no) back to backend via POST /status
     */
    @Autowired
    private LoggingService loggingService;

    @Autowired
    private PollCommandService pollCommandService;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    /**
     * Poll endpoint: returns command and, for DISPENSE, the intake/slot ids to report back in POST /status.
     * Response JSON: { "command": "IDLE" | "DISPENSE" | "SNOOZE", "intakeIds": [1, 2] }
     */
    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll(@RequestParam String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Unknown device"));

        User owner = device.getOwner();
        var result = pollCommandService.getPollingResults(owner);

        Map<String, Object> body = new HashMap<>();
        body.put("command", result.getCommand());
        body.put("intakeIds", result.getIntakeIds());
        body.put("slotNumber", result.getSlotNumber());
        return ResponseEntity.ok(body);
    }

    /**
     * Lets the mobile app set a one-shot SNOOZE for the next poll. DISPENSE cannot be set by the app; it is only triggered by the schedule.
     * Body: { "command": "SNOOZE" }
     */
    @PostMapping("/command")
    public ResponseEntity<String> setCommand(@RequestBody Map<String, String> body) {
        String command = body != null ? body.get("command") : null;
        pollCommandService.setPendingCommand(command);
        return ResponseEntity.ok("Command set");
    }

    // Firmware gets the current schedule to dispense (alternative to poll; not used by current firmware)
    @GetMapping("/schedule")
    public ResponseEntity<Object> getSchedule() {
        // TODO: Fetch schedule
        // TODO: Return schedule in format firmware can understand

        return ResponseEntity.ok("TODO: return schedule for firmware");
    }

    // Firmware sends vitamin intake status (boolean yes/no)
    /*
    Example json:
            {
              "intakeIds": [12345,234,23,355]
              "dispenseEventStatus": true
            }

     Notes: the controller only calls the domain service (and not the repos directly)
     */
    @PostMapping("/status")
    public ResponseEntity<String> reportStatus(@RequestBody IntakeReport request, @RequestParam(defaultValue = "DISPENSER_001") String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Unknown device"));
        log.info("Intake IDs: " + request.getIntakeIds());
        log.info("Dispense status: " + request.getDispenseEventStatus());
        loggingService.handleStatus(request.getIntakeIds(),
                    request.getDispenseEventStatus(), device.getOwner());
        return ResponseEntity.ok("Status processing completed");
    }

}