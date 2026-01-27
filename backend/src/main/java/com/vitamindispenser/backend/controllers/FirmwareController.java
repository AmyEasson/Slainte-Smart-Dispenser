package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.dto.VitaminStatusRequest;
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
    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> reportStatus(@RequestBody VitaminStatusRequest request){
        // TODO: Save intake log to database
        // TODO: Make this data available for mobile app to fetch

        Map<String, String> response = new HashMap<>();
        response.put("message", "Status received successfully");
        return ResponseEntity.ok(response);
    }
}
