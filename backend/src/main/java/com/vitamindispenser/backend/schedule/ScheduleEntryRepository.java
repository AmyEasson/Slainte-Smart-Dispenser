package com.vitamindispenser.backend.schedule;

import com.vitamindispenser.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Integer> {
    List<ScheduleEntry> findByUser(User user);
    List<ScheduleEntry> findByIdIn(List<Integer> ids);
    List<ScheduleEntry> findByUserAndDayAndTime(User user, String day, String time);
    void deleteByUser(User user);
}
