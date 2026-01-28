package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.DispenseEventLog;
import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.repository.DispenseEventLogRepository;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DispenseStatusService {

    private final ScheduleRepository scheduleRepository;
    private final DispenseEventLogRepository logRepository; // or loggingService

    public DispenseStatusService(ScheduleRepository scheduleRepository,
                                 DispenseEventLogRepository logRepository) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
    }

    public void handleStatus(@NonNull List<Integer> intakeIds, @NonNull Boolean taken) {
        // TODO: Find the data for the intake event based on the intake id. ==> done.
        List<DispenseEvent> events = scheduleRepository.findByIds(intakeIds);
        for (DispenseEvent e : events) {
            e.setTaken(taken);
        }
        logEvents(events);
    }
    public void logEvents(List<DispenseEvent> events) {
        // TODO: Persist the data in the database ==> done
        List<DispenseEventLog> rows = events.stream().map(e -> {
            DispenseEventLog log = new DispenseEventLog();
            log.setIntakeId(e.getId());
            log.setVitaminType(e.getVitaminType());
            log.setNumberOfPills(e.getNumberOfPills());
            log.setDay(e.getDay());
            log.setTime(e.getTime());
            log.setTaken(e.getTaken());
            return log;
        }).toList();
        logRepository.saveAll(rows);
    }
}

// to do:
// 1) fix the controller endpoint
// 2) implement the data processing pipeline?
// 3) get that information into the other endpoint.
// 4) how do we model first attempt, second attempt, third attempt ..etc to cover all the 20 minutes?