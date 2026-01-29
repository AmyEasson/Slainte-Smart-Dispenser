package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.FirmwareScheduleService;
import com.vitamindispenser.backend.domain.IntakeRecordingService;
import com.vitamindispenser.backend.dto.logging.FirmwareDispenseResponse;
import com.vitamindispenser.backend.dto.logging.VitaminStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/firmware")
public class FirmwareController {
    private final FirmwareScheduleService firmwareScheduleService;
    private final IntakeRecordingService intakeRecordingService;

    public FirmwareController(
            IntakeRecordingService intakeRecordingService,
            FirmwareScheduleService firmwareScheduleService
    ){
        this.intakeRecordingService = intakeRecordingService;
        this.firmwareScheduleService = firmwareScheduleService;
    }

    // Firmware gets schedules that are due right now (within 5 minute window)
    @GetMapping("/schedule")
    public ResponseEntity<List<FirmwareDispenseResponse>> getSchedule() {
        log.info("Firmware requesting schedule");
        return ResponseEntity.ok(
                firmwareScheduleService.getPendingDispenses(Instant.now())
        );
    }

    // Firmware reports whether vitamins were successfully taken
    @PostMapping("/status")
    public ResponseEntity<String> reportStatus(@RequestBody VitaminStatusRequest request) {
        log.info("Received status report from firmware");
        log.info("Intake IDs: " + request.getIntakeIds());
        log.info("Dispense status: " + request.getDispenseEventStatus());
        intakeRecordingService.handleStatus(
                request.getIntakeIds(),
                request.getDispenseEventStatus()
        );
        log.info("Status processing completed");
        return ResponseEntity.ok("Status processing completed");
    }
}
