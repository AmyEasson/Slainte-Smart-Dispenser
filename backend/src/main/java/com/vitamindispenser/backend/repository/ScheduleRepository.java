package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;

import java.util.Optional;

public interface ScheduleRepository {
    Optional<DispenseEvent> findById(Long id);
}
