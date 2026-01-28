package com.vitamindispenser.backend.controllers;

import com.vitamindispenser.backend.domain.DispenseStatusService;
import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.dto.logging.VitaminStatusRequest;
import com.vitamindispenser.backend.repository.CsvScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
              "intakeIds": [12345,234,23,355]
              "dispenseEventStatus": true
            }

     Notes: the controller only calls the domain service (and not the repos directly)
     */
    @PostMapping("/status")
    public void reportStatus(@RequestBody VitaminStatusRequest request) {
            dispenseStatusService.handleStatus(request.getIntakeIds(),
                    request.getDispenseEventStatus());
    }
}
