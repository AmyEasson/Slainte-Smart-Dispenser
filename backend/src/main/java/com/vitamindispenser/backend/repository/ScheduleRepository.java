package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.Log;
import com.vitamindispenser.backend.dto.schedule.DispenseEvent;

import java.util.List;

public interface ScheduleRepository {
    /* This method is important for the handleStatus method.
       It's used to fetch the relevant information for the pills so they
       get displayed on the dashboard.
    * */
    List<DispenseEvent> findByIds(List<Integer> ids);

    /** Returns all schedule entries (for poll command logic). */
    List<DispenseEvent> findAll();

    List<DispenseEvent> saveAll(List<DispenseEvent> events);
}
