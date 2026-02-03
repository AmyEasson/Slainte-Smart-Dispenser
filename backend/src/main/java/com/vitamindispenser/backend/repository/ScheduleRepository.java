package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.schedule.DispenseEvent;

import java.util.List;

public interface ScheduleRepository {
    List<DispenseEvent> findByIds(List<Integer> ids);

    /** Returns all schedule entries (for poll command logic). */
    List<DispenseEvent> findAll();

    List<DispenseEvent> saveAll(List<DispenseEvent> events);
}
