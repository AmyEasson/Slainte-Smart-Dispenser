package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.IntakeService;
import com.vitamindispenser.backend.domain.ScheduleService;
import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.dto.logging.IntakeLogResponse;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mobile")
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
    private final IntakeService intakeService;
    private final ScheduleService scheduleService;

    public MobileAppController(IntakeService intakeService,
                               ScheduleService scheduleService){
        this.intakeService = intakeService;
        this.scheduleService = scheduleService;
    }

    // Mobile app sends schedule with multiple vitamins
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, String>> createSchedule(@RequestBody ScheduleRequest request) {
        scheduleService.createSchedule(request);
        return ResponseEntity.ok(
                Map.of("message", "Schedule created successfully")
        );
    }

    // Mobile app gets vitamin intake data
    @GetMapping("/intake")
    public ResponseEntity<List<IntakeLogResponse>> getIntakeData() {
        return ResponseEntity.ok(intakeService.getHistory());
    }
}
