package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.model.ScheduleEntry;
import com.vitamindispenser.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Integer> {
    List<ScheduleEntry> findByUser(User user);
    List<ScheduleEntry> findByIdIn(List<Integer> ids);
    void deleteByUser(User user);
}
