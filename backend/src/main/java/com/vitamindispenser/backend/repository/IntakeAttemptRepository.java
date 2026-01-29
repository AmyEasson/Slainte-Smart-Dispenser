package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NOTE:
 * This repository is currently in-memory.
 * Intake history is reset when the backend restarts.
 * This will be replaced with a database-backed repository later.
 */

@Repository
public class IntakeAttemptRepository {
    private final Map<Integer, IntakeAttempt> attempts = new HashMap<>();
    public IntakeAttempt findLatest(Integer intakeId) {
        return attempts.get(intakeId);
    }

    public void save(IntakeAttempt attempt) {
        attempts.put(attempt.getIntakeId(), attempt);
    }

    public List<IntakeAttempt> findAll() {
        return new ArrayList<>(attempts.values());
    }
}
