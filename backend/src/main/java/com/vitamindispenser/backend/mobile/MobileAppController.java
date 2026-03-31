package com.vitamindispenser.backend.mobile;

import com.vitamindispenser.backend.auth.AccountService;
import com.vitamindispenser.backend.exceptions.DeviceAlreadyClaimedException;
import com.vitamindispenser.backend.exceptions.DeviceNotFoundException;
import com.vitamindispenser.backend.exceptions.UserNotFoundException;
import com.vitamindispenser.backend.logging.LoggingExportService;
import com.vitamindispenser.backend.schedule.SchedulingService;
import com.vitamindispenser.backend.logging.dto.IntakeForRawDashboard;
import com.vitamindispenser.backend.logging.dto.IntakeResponseForRawDashboard;
import com.vitamindispenser.backend.schedule.dto.ScheduleRequest;
import com.vitamindispenser.backend.device.Device;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Mobile App", description = "Endpoints for the mobile application")
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
    private final BarcodeService barcodeService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final AccountService accountService;

    public MobileAppController(SchedulingService schedulingService, LoggingExportService exportService, UserRepository userRepository, DeviceRepository deviceRepository, BarcodeService barcodeService, AccountService accountService) {
        this.schedulingService = schedulingService;
        this.exportService = exportService;
        this.userRepository = userRepository;
        this.barcodeService = barcodeService;
        this.deviceRepository = deviceRepository;
        this.accountService = accountService;
    }

    /**
     * ties the user account and therefore their schedule to a physical dispenser by their ids
     * @param body device Id
     * @param principal the JWT token of the authenticated user
     * @return success or failure message only
     */
    @Operation(summary = "Claim device", description = "Links a physical dispenser to the authenticated user's account by device ID")
    @PostMapping("/claim-device")
    public ResponseEntity<String> claimDevice(@RequestBody Map<String, String> body, Principal principal) {
        String deviceId = body.get("deviceId");
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        if (device.getOwner() != null && !device.getOwner().getId().equals(user.getId())) {
            throw new DeviceAlreadyClaimedException(deviceId);
        }

        device.setOwner(user);
        deviceRepository.save(device);

        return ResponseEntity.ok("Successfully claimed device");
    }

    @Operation(summary = "Check device claimed", description = "Returns whether the authenticated user has a device claimed — used post-login to decide whether to show the setup flow")
    @GetMapping("/has-device")
    public ResponseEntity<?> hasDevice(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        boolean hasDevice = deviceRepository.findByOwner(user).isPresent();
        return ResponseEntity.ok(Map.of("hasDevice", hasDevice));
    }

    @Operation(summary = "Save schedule", description = "Creates or overwrites the user's vitamin dispensing schedule")
    @PostMapping("/schedule")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequest request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        try {
            schedulingService.saveSchedule(request, user);
            return ResponseEntity.ok("Schedule has been sent");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * fetches the user's currently stored schedule
     * @param principal the JWT token of the authenticated user
     * @return the user's currently active schedule
     */
    @Operation(summary = "Get schedule", description = "Returns the user's currently active dispensing schedule")
    @GetMapping("/getSchedule")
    public ResponseEntity<ScheduleRequest> getSchedule(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        return ResponseEntity.ok(schedulingService.retrieveSchedule(user));
    }

    /**
     * gets a list of which vitamins should be in each slot number, to
     * display to the user in the loading guide
     * @param principal the JWT token of the authenticated user
     * @return the list of slots and what vitamins should be in each
     */
    @Operation(summary = "Get slots", description = "Returns which vitamins should be loaded into each dispenser slot")
    @GetMapping("/slots")
    public ResponseEntity<?> getSlots(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        return ResponseEntity.ok(schedulingService.getSlots(user));
    }

    /**
     * returns the refill date for the user's current schedule
     * @param principal the JWT-token of the authenticated user
     * @return the user's refill information
     */
    @Operation(summary = "Get refill info", description = "Returns the date the dispenser needs refilling based on the current schedule")
    @GetMapping("/slots/refill-info")
    public ResponseEntity<?> getRefillIndoors(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        return ResponseEntity.ok(schedulingService.getRefillInfo(user));
    }

    @Operation(summary = "Confirm refill", description = "Marks the dispenser as refilled and resets the refill countdown")
    @PostMapping("/slots/confirm-fill")
    public ResponseEntity<?> confirmFill(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        return ResponseEntity.ok(schedulingService.confirmFill(user));
    }

    @Operation(summary = "Export intake logs as CSV", description = "Downloads all vitamin intake history for the authenticated user as a CSV file")
    @GetMapping("/logs/export.csv")
    public ResponseEntity<String> exportCsv(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        String csv = exportService.exportAllLogsAsCsv(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @Operation(summary = "Get intake logs", description = "Returns raw intake log data for the dashboard")
    @GetMapping("/intake")
    public ResponseEntity<?> getIntake(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
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

    @Operation(summary = "Lookup barcode", description = "Looks up vitamin product information by barcode for automatic schedule entry")
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> lookupBarcode(@PathVariable String barcode) {
        Map<String, Object> result = barcodeService.lookupBarcode(barcode);
        if (result == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Pause dispensing", description = "Pauses the dispensing schedule — no vitamins will be dispensed until resumed")
    @PostMapping("/pause")
    public ResponseEntity<?> pause(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        user.setPaused(true);
        user.setPausedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok("Dispensing paused");
    }

    @Operation(summary = "Resume dispensing", description = "Resumes the dispensing schedule and calculates any missed slots that need to be advanced")
    @PostMapping("/resume")
    public ResponseEntity<?> resume(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        List<String> missedQueue = schedulingService.calculateMissedSlotQueue(user, user.getPausedAt());
        user.setSlotsToAdvance(missedQueue.size());

        long immediateDispenses = missedQueue.stream()
                .filter(e -> e.endsWith("|DISPENSE"))
                .count();

        long advanceCount = missedQueue.stream()
                .filter(e -> e.endsWith("|ADVANCE"))
                .count();

        try {
            user.setMissedSlotQueue(new ObjectMapper().writeValueAsString(missedQueue));
        } catch (Exception e) {
            user.setMissedSlotQueue("[]");
        }

        user.setPaused(false);
        user.setPausedAt(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Dispensing resumed",
                "slotsToAdvance", advanceCount,
                "immediateDispenses", immediateDispenses
        ));
    }

    @Operation(summary = "Get pause status", description = "Returns whether dispensing is currently paused")
    @GetMapping("/pause-status")
    public ResponseEntity<?> pauseStatus(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of("paused", user.isPaused()));
    }

    @Operation(summary = "Change password", description = "Updates the authenticated user's password after verifying their current password")
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> body, Principal principal) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));

        try {
            accountService.changePassword(user, oldPassword, newPassword);
            return ResponseEntity.ok("Password updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete account", description = "Permanently deletes the authenticated user's account including all schedule entries, intake logs and device claim")
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        accountService.deleteAccount(user);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @Operation(summary = "Empty carousel", description = "Flags the dispenser to rotate all slots out on the next firmware poll")
    @PostMapping("/empty-carousel")
    public ResponseEntity<?> emptyCarousel(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        schedulingService.requestEmpty(user);
        return ResponseEntity.ok("Empty command queued");
    }
}
