package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.IntakeHistoryService;
import com.vitamindispenser.backend.domain.ScheduleService;
import com.vitamindispenser.backend.dto.logging.IntakeLogResponse;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     *         {
     *           "day": "monday",
     *           "times": [
     *             {"id": 1, "time": "10:00"},
     *             {"id": 2, "time": "12:00"}
     *           ]
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    private final IntakeHistoryService intakeHistoryService;
    private final ScheduleService scheduleService;

    public MobileAppController(IntakeHistoryService intakeHistoryService,
                               ScheduleService scheduleService){
        this.intakeHistoryService = intakeHistoryService;
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
        return ResponseEntity.ok(intakeHistoryService.getHistory());
    }
}
