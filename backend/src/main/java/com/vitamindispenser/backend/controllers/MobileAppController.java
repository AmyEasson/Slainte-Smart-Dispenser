package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.dto.logging.IntakeLogResponse;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile")
public class MobileAppController {
    /*
     * Expected JSON structure from mobile app:
     * {
     *   "vitamins": [
     *     {
     *       "vitaminType": "Vitamin A",
     *       "numberOfPills": 2,
     *       "schedule": [
     *         {"day": "monday", "times": ["10:00", "12:00"]},
     *         {"day": "tuesday", "times": ["10:00", "12:00"]},
     *         {"day": "wednesday", "times": ["10:00", "12:00"]}
     *       ]
     *     },
     *     {
     *       "vitaminType": "Magnesium",
     *       "numberOfPills": 1,
     *       "schedule": [
     *         {"day": "monday", "times": ["09:00"]}
     *       ]
     *     }
     *   ]
     * }
     */

    // Mobile app sends schedule with multiple vitamins
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, String>> createSchedule(@RequestBody ScheduleRequest request) {
        // TODO: Save schedule
        // TODO: Signal firmware that new schedule is available

        Map<String, String> response = new HashMap<>();
        response.put("message", "Schedule created successfully");
        return ResponseEntity.ok(response);
    }

    // Mobile app gets vitamin intake data
    @GetMapping("/intake")
    public ResponseEntity<List<IntakeLogResponse>> getIntakeData() {
        // TODO: Fetch intake logs

        // For now returning an empty list
        return ResponseEntity.ok(new ArrayList<>());
    }
}
