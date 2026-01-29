package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.FirmwareDispenseResponse;
import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import com.vitamindispenser.backend.dto.logging.IntakeStatus;
import com.vitamindispenser.backend.dto.schedule.DispenseSchedule;
import com.vitamindispenser.backend.repository.IntakeAttemptRepository;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DISPENSE FLOW OVERVIEW
 *
 * 1. Mobile app defines schedules → stored as DispenseSchedule
 *
 * 2. Firmware polls /api/v1/firmware/schedule
 *    - We find all DispenseSchedule entries that are due "now"
 *    - For each due schedule:
 *        - Create an IntakeAttempt (status = PENDING)
 *        - Return FirmwareDispenseResponse to firmware
 *
 * 3. Firmware performs physical dispensing and monitoring
 *
 * 4. Firmware reports result via /api/v1/firmware/status
 *    - DispenseStatusService updates the latest IntakeAttempt
 *      to TAKEN or MISSED
 *
 * 5. Mobile app reads IntakeAttempt history via /api/v1/mobile/intake
 *
 * IMPORTANT:
 * - DispenseSchedule = planned event
 * - IntakeAttempt   = runtime execution + outcome
 * - Firmware never modifies schedules
 */


@Service
public class DispenseService {
    private final ScheduleRepository scheduleRepository;
    private final IntakeAttemptRepository attemptRepository;

    public DispenseService(ScheduleRepository scheduleRepository, IntakeAttemptRepository attemptRepository) {
        this.scheduleRepository = scheduleRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<FirmwareDispenseResponse> getPendingDispenses(Instant now){
        List<DispenseSchedule> due = scheduleRepository.findDue(now);
        List<FirmwareDispenseResponse> result = new ArrayList<>();

        for (DispenseSchedule entry: due){
            // create runtime attempt
            IntakeAttempt attempt = new IntakeAttempt();
            attempt.setIntakeId(entry.getId());
            attempt.setStatus(IntakeStatus.PENDING);
            attempt.setScheduledAt(now);
            attemptRepository.save(attempt);

            // tell firmware what to dispense
            FirmwareDispenseResponse dto = new FirmwareDispenseResponse();
            dto.setIntakeId(entry.getId());
            dto.setVitaminType(entry.getVitaminType());
            dto.setNumberOfPills(entry.getNumberOfPills());
            result.add(dto);
        }
        return result;
    }
}
