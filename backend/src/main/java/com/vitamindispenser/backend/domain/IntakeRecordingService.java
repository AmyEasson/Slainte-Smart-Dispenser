package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import com.vitamindispenser.backend.dto.logging.IntakeStatus;
import com.vitamindispenser.backend.repository.IntakeAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IntakeRecordingService {

    private final IntakeAttemptRepository attemptRepository;

    public IntakeRecordingService(IntakeAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public void handleStatus(List<Integer> intakeIds, boolean taken) {
        for (Integer id : intakeIds) {
            // Find the most recent attempt for this intakeId
            Optional<IntakeAttempt> attemptOpt = attemptRepository
                    .findFirstByIntakeIdOrderByScheduledAtDesc(id);

            if (attemptOpt.isEmpty()) {
                log.warn("No intake attempt found for ID: {}", id);
                continue;
            }

            IntakeAttempt attempt = attemptOpt.get();
            attempt.setStatus(taken ? IntakeStatus.TAKEN : IntakeStatus.MISSED);
            attempt.setReportedAt(Instant.now());

            attemptRepository.save(attempt);
        }
    }
}