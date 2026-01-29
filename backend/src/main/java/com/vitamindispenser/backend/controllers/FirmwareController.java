package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.DispenseService;
import com.vitamindispenser.backend.domain.DispenseStatusService;
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
    /*
     * Firmware workflow:
     * 1. Firmware polls this endpoint to get current schedule
     * 2. Firmware dispenses vitamins at scheduled times
     * 3. Firmware sends boolean (vitamin taken yes/no) back to backend
     */
    private final DispenseService dispenseService;
    private final DispenseStatusService dispenseStatusService;

    public FirmwareController(
            DispenseStatusService dispenseStatusService,
            DispenseService dispenseService
    ){
        this.dispenseStatusService = dispenseStatusService;
        this.dispenseService = dispenseService;
    }

    // Firmware gets the current schedule to dispense
    @GetMapping("/schedule")
    public ResponseEntity<List<FirmwareDispenseResponse>> getSchedule() {
        log.info("Firmware requesting schedule");
        return ResponseEntity.ok(
                dispenseService.getPendingDispenses(Instant.now())
        );
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
    public ResponseEntity<String> reportStatus(@RequestBody VitaminStatusRequest request) {
        log.info("Received status report from firmware");
        log.info("Intake IDs: " + request.getIntakeIds());
        log.info("Dispense status: " + request.getDispenseEventStatus());
        dispenseStatusService.handleStatus(
                request.getIntakeIds(),
                request.getDispenseEventStatus()
        );
        log.info("Status processing completed");
        return ResponseEntity.ok("Status processing completed");
    }
}
