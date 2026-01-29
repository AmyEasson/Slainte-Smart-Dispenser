package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.dto.schedule.DaySchedule;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import com.vitamindispenser.backend.dto.schedule.TimeSlot;
import com.vitamindispenser.backend.dto.schedule.VitaminSchedule;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<DispenseEvent> events = convertToDispenseEvents(request);
        scheduleRepository.saveAll(events);
    }

    /**
     * Gets all schedules for firmware
     */
    public List<DispenseEvent> convertToDispenseEvents(ScheduleRequest request) {
        List<DispenseEvent> events = new ArrayList<>();
        for (VitaminSchedule vitamin : request.getVitamins()){
            for (DaySchedule daySchedule : vitamin.getSchedule()){
                for (TimeSlot timeSlot : daySchedule.getTimes()){
                    // create one DispenseEvent per vitamin-day-time combination
                    DispenseEvent event = new DispenseEvent(
                            vitamin.getNumberOfPills(),
                            vitamin.getVitaminType(),
                            daySchedule.getDay(),
                            timeSlot.getTime(),
                            false,
                            timeSlot.getId()
                    );
                    events.add(event);
                }
            }
        }
        return events;
    }
}
