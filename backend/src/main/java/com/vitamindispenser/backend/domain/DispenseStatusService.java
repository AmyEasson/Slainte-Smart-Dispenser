package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import com.vitamindispenser.backend.dto.logging.IntakeStatus;
import com.vitamindispenser.backend.repository.IntakeAttemptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DispenseStatusService {

    private final IntakeAttemptRepository attemptRepository;

    public DispenseStatusService(IntakeAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public void handleStatus(List<Integer> intakeIds, boolean taken) {

        for (Integer id : intakeIds) {
            IntakeAttempt attempt = attemptRepository.findLatest(id);

            attempt.setStatus(taken ? IntakeStatus.TAKEN : IntakeStatus.MISSED);
            attempt.setReportedAt(Instant.now());

            attemptRepository.save(attempt);
        }
    }
}


// to do:
// 1) fix the controller endpoint
// 2) implement the data processing pipeline?
// 3) get that information into the other endpoint.
// 4) how do we model first attempt, second attempt, third attempt ..etc to cover all the 20 minutes?