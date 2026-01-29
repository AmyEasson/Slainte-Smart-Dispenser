package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.DispenseStatusService;
import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.dto.logging.VitaminStatusRequest;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/firmware")
public class FirmwareController {
    /*
     * Firmware workflow:
     * 1. Firmware polls this endpoint to get current schedule
     * 2. Firmware dispenses vitamins at scheduled times
     * 3. Firmware sends boolean (vitamin taken yes/no) back to backend
     */
    @Autowired
    private DispenseStatusService dispenseStatusService;
    @Autowired
    private ScheduleRepository scheduleRepository;

    // Firmware gets the current schedule to dispense
    @GetMapping("/schedule")
    public ResponseEntity<List<DispenseEvent>> getSchedule() {
        log.info("Firmware requesting schedule");
        // TODO: Get all schedule IDs (for now, just which IDs exist)
        // Currently a placeholder, need to implement some findAll() function
        List<Integer> allIds = List.of(1,2,3); //temporarily hardcoded

        List<DispenseEvent> events = scheduleRepository.findByIds(allIds);
        log.info("Returning {} schedule events", events.size());
        return ResponseEntity.ok(events);
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
        dispenseStatusService.handleStatus(request.getIntakeIds(),
                    request.getDispenseEventStatus());
        log.info("Status processing completed");
        return ResponseEntity.ok("Status processing completed");
    }
}
