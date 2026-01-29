package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;

import java.util.List;

public interface ScheduleRepository {
    List<DispenseEvent> findByIds(List<Integer> ids);
    List<DispenseEvent> findAll();
    void saveAll(List<DispenseEvent> events);
}
