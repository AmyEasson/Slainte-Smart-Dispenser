package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.IntakeLogResponse;
import com.vitamindispenser.backend.repository.IntakeAttemptRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntakeService {
    private final IntakeAttemptRepository attemptRepository;

    public IntakeService(IntakeAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public List<IntakeLogResponse> getHistory(){
        return attemptRepository.findAll().stream()
                .map(attempt -> {
                    IntakeLogResponse r = new IntakeLogResponse();
                    r.setIntakeId(attempt.getIntakeId());
                    r.setStatus(attempt.getStatus());
                    r.setReportedAt(attempt.getReportedAt());
                    return r;
                })
                .toList();
    }

}
