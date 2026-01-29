package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.schedule.*;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Gets all schedules for firmware
     */
    public void createSchedule(ScheduleRequest request){
        List<DispenseSchedule> schedules = convertToDispenseSchedules(request);
        scheduleRepository.saveAll(schedules);
    }

    /**
     * Converts mobile app's nested schedule format into flat DispenseSchedule entries.
     * Each vitamin-day-time combination becomes one DispenseSchedule row in CSV.
     */
    public List<DispenseSchedule> convertToDispenseSchedules(ScheduleRequest request) {
        List<DispenseSchedule> schedules = new ArrayList<>();

        for (VitaminSchedule vitamin: request.getVitamins()){
            for (DaySchedule day: vitamin.getSchedule()){
                for (TimeSlot slot : day.getTimes()){
                    DispenseSchedule schedule = new DispenseSchedule();
                    schedule.setId(slot.getId());
                    schedule.setVitaminType(vitamin.getVitaminType());
                    schedule.setNumberOfPills(vitamin.getNumberOfPills());
                    schedule.setDay(DayOfWeek.valueOf(day.getDay().toUpperCase()));
                    schedule.setTime(LocalTime.parse(slot.getTime()));

                    schedules.add(schedule);
                }
            }
        }
        return schedules;
    }
}
