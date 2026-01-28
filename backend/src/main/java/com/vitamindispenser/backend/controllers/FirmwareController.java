package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.dto.logging.VitaminStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/firmware")
public class FirmwareController {
    /*
     * Firmware workflow:
     * 1. Firmware polls this endpoint to get current schedule
     * 2. Firmware dispenses vitamins at scheduled times
     * 3. Firmware sends boolean (vitamin taken yes/no) back to backend
     */

    // Firmware gets the current schedule to dispense
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
              "intakeId": 12345,
              "taken": true
            }
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> reportStatus(@RequestBody VitaminStatusRequest request) {
        // TODO:
        // Steps:1) Find the data for the intake event based on the intake id.
        // in the database every intake has an id [but dto exactly?] vitaminType, number of pills, day, timestamp;
        // 2) Update the database status with all the data available so far

        Map<String, String> response = new HashMap<>();
        response.put("message", "Status received successfully");
        response.put("intakeId", String.valueOf(request.getIntakeId()));
        response.put("taken", String.valueOf(request.getVitaminTaken()));
        return ResponseEntity.ok(response);
    }
}
