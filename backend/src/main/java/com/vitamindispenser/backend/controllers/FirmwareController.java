package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.DispenseStatusService;
import com.vitamindispenser.backend.dto.logging.VitaminStatusRequest;
import lombok.NonNull;
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
     * 1. Firmware polls "/poll" every ~2 seconds to see if it needs to act (Dispense/Snooze).
     * 2. If valid command received, Firmware executes hardware logic.
     * 3. Firmware sends boolean status (taken/missed) back to "/status".
     */

    @Autowired
    private DispenseStatusService dispenseStatusService;

    // Volatile ensures thread visibility across requests. 
    // In a production app, this state should ideally live in a Database or Redis.
    private volatile String pendingCommand = "IDLE";

    // -------------------------------------------------------------------------
    // 1. ARDUINO POLLING INTERFACE
    // -------------------------------------------------------------------------

    // Firmware calls this periodically (e.g., every 2s) to check for work
    @GetMapping("/poll")
    public Map<String, String> pollForCommands() {
        Map<String, String> response = new HashMap<>();
        response.put("command", pendingCommand);

        // If a command was waiting (e.g., "DISPENSE"), we deliver it now
        // and immediately reset to "IDLE" so it doesn't trigger twice.
        if (!"IDLE".equals(pendingCommand)) {
            log.info("Command delivered to firmware: {}", pendingCommand);
            pendingCommand = "IDLE";
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // 2. FRONTEND COMMAND INTERFACE
    // -------------------------------------------------------------------------

    // Frontend (Web App) calls this to queue a command for the Arduino
    // Example: User clicks "Snooze" or "Dispense Now" on the dashboard
    @PostMapping("/command")
    public ResponseEntity<String> setCommand(@RequestParam String cmd) {
        // cmd could be "DISPENSE", "SNOOZE", "RESET"
        this.pendingCommand = cmd;
        log.info("Command queued by user: {}", cmd);
        return ResponseEntity.ok("Command Queued: " + cmd);
    }

    // -------------------------------------------------------------------------
    // 3. STATUS & REPORTING
    // -------------------------------------------------------------------------

    // Firmware sends vitamin intake status (boolean yes/no)
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

    // -------------------------------------------------------------------------
    // 4. SCHEDULE MANAGEMENT (Optional / Future Use)
    // -------------------------------------------------------------------------

    // If you want the firmware to download the full day's plan in advance
    @GetMapping("/schedule")
    public ResponseEntity<Object> getSchedule() {
        // TODO: Fetch schedule from DB
        // TODO: Return schedule in JSON format firmware can parse
        return ResponseEntity.ok("TODO: return schedule for firmware");
    }
}
