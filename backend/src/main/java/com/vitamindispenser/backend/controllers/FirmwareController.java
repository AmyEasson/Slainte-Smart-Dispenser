package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.logging.DispenseStatusService;
import com.vitamindispenser.backend.domain.schedule.PollCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
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
    private DispenseStatusService dispenseStatusService;

    @Autowired
    private PollCommandService pollCommandService;

    /**
     * Poll endpoint: returns command and, for DISPENSE, the intake/slot ids to report back in POST /status.
     * Response JSON: { "command": "IDLE" | "DISPENSE" | "SNOOZE", "intakeIds": [1, 2] }
     */
    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll() {
        var result = pollCommandService.getPollingResults();
        Map<String, Object> body = new HashMap<>();
        body.put("command", result.getCommand());
        body.put("intakeIds", result.getIntakeIds());
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
    /**
    @PostMapping("/status")
    public ResponseEntity<String> reportStatus(@RequestBody VitaminStatusRequest request) {
        log.info("Received status report from firmware");
        log.info("Intake IDs: " + request.getIntakeIds());
        log.info("Dispense status: " + request.getDispenseEventStatus());
        dispenseStatusService.handleStatus(request.getIntakeIds(),
                    request.getDispenseEventStatus());
        log.info("Status processing completed");
        return ResponseEntity.ok("Status processing completed");
    }
    */
}
