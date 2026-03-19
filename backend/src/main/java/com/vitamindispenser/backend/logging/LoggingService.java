package com.vitamindispenser.backend.logging;

import com.vitamindispenser.backend.exceptions.ScheduleNotFoundException;
import com.vitamindispenser.backend.logging.dto.Log;
import com.vitamindispenser.backend.schedule.ScheduleEntry;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoggingService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final DispenseEventLogRepository logRepository; // or loggingService

    public LoggingService(ScheduleEntryRepository scheduleEntryRepository,
                          DispenseEventLogRepository logRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.logRepository = logRepository;
    }

    /*
    This method needs to fetch the information related the pills that have been dispensed.
    That information comes from the scheduling database; hence why the scheduleRepository is used.
     */
    public void handleStatus(@NonNull List<Integer> intakeIds, @NonNull Boolean taken, User user) {
        if (intakeIds.isEmpty()) {
            throw new IllegalArgumentException("intakeIds must not be empty");
        }
        List<ScheduleEntry> entries = scheduleEntryRepository.findByIdIn(intakeIds);
        if (entries.isEmpty()) {
            throw new ScheduleNotFoundException("No schedule events found for ids: " + intakeIds);
        }

        List<Log> logs = new ArrayList<>();
        for (ScheduleEntry e : entries) {
            Log log = fromScheduleEntry(e);
            log.setTaken(taken);
            logs.add(log);
        }
        logEvents(logs, user);
    }

    public Log fromScheduleEntry(ScheduleEntry entry) {
        if (entry == null) return null;
        Log log = new Log();
        log.setVitaminType(entry.getVitaminType());
        log.setDate(LocalDate.now());
        log.setDay(entry.getDay());
        log.setTime(entry.getTime());
        log.setNumberOfPills(entry.getNumberOfPills());
        log.setId(entry.getId());
        log.setTaken(false);
        return log;
    }

    public void logEvents(List<Log> events, User user) {
        List<LoggingDatabase> rows = events.stream().map(e -> {
            LoggingDatabase log = new LoggingDatabase();
            log.setIntakeId(e.getId());
            log.setVitaminType(e.getVitaminType());
            log.setNumberOfPills(e.getNumberOfPills());
            log.setDate(LocalDate.now());
            log.setDay(e.getDay());
            log.setTime(e.getTime());
            log.setTaken(e.getTaken());
            log.setUser(user);
            return log;
        }).toList();
        logRepository.saveAll(rows);
    }
}