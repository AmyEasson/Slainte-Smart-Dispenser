package com.vitamindispenser.backend.domain.logging;

import com.vitamindispenser.backend.dto.logging.LoggingDatabase;
import com.vitamindispenser.backend.dto.logging.Log;
import com.vitamindispenser.backend.dto.schedule.DispenseEvent;
import com.vitamindispenser.backend.repository.DispenseEventLogRepository;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoggingService {

    private final ScheduleRepository scheduleRepository;
    private final DispenseEventLogRepository logRepository; // or loggingService

    public LoggingService(ScheduleRepository scheduleRepository,
                          DispenseEventLogRepository logRepository) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
    }

    /*
    This method needs to fetch the information related the pills that have been dispensed.
    That information comes from the scheduling database; hence why the scheduleRepository is used.
     */
    public void handleStatus(@NonNull List<Integer> intakeIds, @NonNull Boolean taken) {
        // find info for all the ids
        List<DispenseEvent> events = scheduleRepository.findByIds(intakeIds);
        // create logs to log those info along with their ids
        List<Log> logs = new ArrayList<>();
        // we need to make a Log from the Dispense Event
        for (DispenseEvent e: events) {
            Log log = fromDispenseEvent(e);
            log.setTaken(taken);
            logs.add(log);
        }
        logEvents(logs);
    }

    public Log fromDispenseEvent(DispenseEvent event) {
        if (event == null) {
            return null;
        }

        Log log = new Log();
        log.setVitaminType(event.getVitaminType());
        log.setDay(event.getDay());
        log.setTime(event.getTime());
        log.setNumberOfPills(event.getNumberOfPills());
        log.setId(event.getId());
        log.setTaken(false);

        return log;
    }

    public void logEvents(List<Log> events) {
        List<LoggingDatabase> rows = events.stream().map(e -> {
            LoggingDatabase log = new LoggingDatabase();
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