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
    @Autowired
    private ScheduleRepository scheduleRepository;

    /**
     * Converts mobile app schedule format to DispenseEvent format and saves to CSV
     */
    public void createSchedule(ScheduleRequest request){
        List<DispenseSchedule> schedules = convertToDispenseSchedules(request);
        scheduleRepository.saveAll(schedules);
    }

    /**
     * Gets all schedules for firmware
     */
    public List<DispenseSchedule> convertToDispenseSchedules(ScheduleRequest request) {
        List<DispenseSchedule> schedules = new ArrayList<>();

        for (VitaminSchedule vitamin: request.getVitamins()){
            for (DaySchedule day: vitamin.getSchedule()){
                for (TimeSlot slot : day.getTimes()){
                    DispenseSchedule schedule = new DispenseSchedule();
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
