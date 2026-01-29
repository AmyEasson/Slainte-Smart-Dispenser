package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.schedule.DispenseSchedule;

import java.time.Instant;
import java.util.List;

public interface ScheduleRepository {
    List<DispenseSchedule> findAll();
    List<DispenseSchedule> findDue(Instant now);
    void saveAll(List<DispenseSchedule> schedules);
}
