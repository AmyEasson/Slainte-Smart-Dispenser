package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.logging.LoggingExportService;
import com.vitamindispenser.backend.domain.schedule.SchedulingService;
import com.vitamindispenser.backend.dto.schedule.DispenseEvent;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
    private final SchedulingService schedulingService;
    private final LoggingExportService exportService;

    public MobileAppController(SchedulingService schedulingService,  LoggingExportService exportService) {
        this.schedulingService = schedulingService;
        this.exportService = exportService;
    }

    // Mobile app sends schedule with multiple vitamins
    @PostMapping("/schedule")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequest request) {
        schedulingService.saveSchedule(request);
        return ResponseEntity.ok("Schedule has been sent");
    }

    // Mobile app gets vitamin intake data
    @GetMapping("/logs/export.csv")
    public ResponseEntity<String> exportCsv() {
        String csv = exportService.exportAllLogsAsCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
