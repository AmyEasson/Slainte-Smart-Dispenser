package com.vitamindispenser.backend.mobile;

import com.vitamindispenser.backend.logging.LoggingExportService;
import com.vitamindispenser.backend.schedule.SchedulingService;
import com.vitamindispenser.backend.logging.dto.IntakeForRawDashboard;
import com.vitamindispenser.backend.logging.dto.IntakeResponseForRawDashboard;
import com.vitamindispenser.backend.schedule.dto.ScheduleRequest;
import com.vitamindispenser.backend.device.Device;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
    private final SchedulingService schedulingService;
    private final LoggingExportService exportService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    public MobileAppController(SchedulingService schedulingService, LoggingExportService exportService, UserRepository userRepository, DeviceRepository deviceRepository) {
        this.schedulingService = schedulingService;
        this.exportService = exportService;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping("/claim-device")
    public ResponseEntity<String> claimDevice(@RequestBody Map<String, String> body, Principal principal){
        String deviceId = body.get("deviceId");
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElse(new Device());
        device.setDeviceId(deviceId);
        device.setOwner(user);
        deviceRepository.save(device);

        return ResponseEntity.ok("Successfully claimed device");
    }

    // Mobile app sends schedule with multiple vitamins
    @PostMapping("/schedule")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequest request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        schedulingService.saveSchedule(request, user);
        return ResponseEntity.ok("Schedule has been sent");
    }

    @GetMapping("/getSchedule")
    public ResponseEntity<ScheduleRequest> getSchedule(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(schedulingService.retrieveSchedule(user));
    }

    @GetMapping("/slots")
    public ResponseEntity<?> getSlots(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(schedulingService.getSlots(user));
    }

    @GetMapping("/slots/refill-info")
    public ResponseEntity<?> getRefillIndoors(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(schedulingService.getRefillInfo(user));
    }

    @PostMapping("/slots/confirm-fill")
    public ResponseEntity<?> confirmFill(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(schedulingService.confirmFill(user));
    }

    // Mobile app gets vitamin intake data
    @GetMapping("/logs/export.csv")
    public ResponseEntity<String> exportCsv(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String csv = exportService.exportAllLogsAsCsv(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    /*
    This endpoint shall be called by the UI to just enumerate raw data there
     */
    @GetMapping("/intake")
    public ResponseEntity<?> getIntake(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<IntakeForRawDashboard> data = exportService.exportDashboardJson(user);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    new IntakeResponseForRawDashboard(List.of(), "No intake data available.")
            );
        }

        return ResponseEntity.ok(
                new IntakeResponseForRawDashboard(data, null)
        );
    }
}
