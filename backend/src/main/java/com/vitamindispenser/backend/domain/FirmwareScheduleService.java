package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.schedule.DispenseSchedule;
import com.vitamindispenser.backend.dto.logging.FirmwareDispenseResponse;
import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import com.vitamindispenser.backend.dto.logging.IntakeStatus;
import com.vitamindispenser.backend.repository.IntakeAttemptRepository;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * DISPENSE FLOW OVERVIEW
 * 1. Mobile app defines schedules → stored as DispenseSchedule
 * 2. Firmware polls /api/v1/firmware/schedule
 *    - We find all DispenseSchedule entries that are due "now"
 *    - For each due schedule:
 *        - Create an IntakeAttempt (status = PENDING)
 *        - Return FirmwareDispenseResponse to firmware
 * 3. Firmware performs physical dispensing and monitoring
 * 4. Firmware reports result via /api/v1/firmware/status
 *    - IntakeRecordingService updates the latest IntakeAttempt
 *      to TAKEN or MISSED
 * 5. Mobile app reads IntakeAttempt history via /api/v1/mobile/intake
 * IMPORTANT:
 * - DispenseSchedule = planned event
 * - IntakeAttempt   = runtime execution + outcome
 * - Firmware never modifies schedules
 */
@Service
public class FirmwareScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final IntakeAttemptRepository attemptRepository;

    public FirmwareScheduleService(ScheduleRepository scheduleRepository,
                                   IntakeAttemptRepository attemptRepository) {
        this.scheduleRepository = scheduleRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<FirmwareDispenseResponse> getPendingDispenses(Instant now){
        List<DispenseSchedule> due = scheduleRepository.findDue(now);
        List<FirmwareDispenseResponse> result = new ArrayList<>();

        // Calculate start and end of today
        Instant startOfDay = now.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        for (DispenseSchedule entry : due){
            // Skip if already created an attempt today
            if (attemptRepository.existsTodayByIntakeId(entry.getId(), startOfDay, endOfDay)) {
                continue;
            }

            // Create runtime attempt
            IntakeAttempt attempt = new IntakeAttempt();
            attempt.setIntakeId(entry.getId());
            attempt.setStatus(IntakeStatus.PENDING);
            attempt.setScheduledAt(now);
            attemptRepository.save(attempt);

            // Tell firmware what to dispense
            FirmwareDispenseResponse dto = new FirmwareDispenseResponse();
            dto.setIntakeId(entry.getId());
            dto.setVitaminType(entry.getVitaminType());
            dto.setNumberOfPills(entry.getNumberOfPills());
            result.add(dto);
        }
        return result;
    }
}