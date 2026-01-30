package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.schedule.DispenseSchedule;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class CsvScheduleRepository implements ScheduleRepository {

    private final Resource csv;

    public CsvScheduleRepository(
            @Value("${schedule.csv.location}") Resource csv
    ) {
        this.csv = csv;
    }

    @Override
    public List<DispenseSchedule> findAll() {
        List<DispenseSchedule> results = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(csv.getInputStream()))) {

            for (CSVRecord r : parser) {
                DispenseSchedule schedule = new DispenseSchedule();
                schedule.setId(Integer.parseInt(r.get("id")));
                schedule.setNumberOfPills(Integer.parseInt(r.get("numberOfPills")));
                schedule.setVitaminType(r.get("vitaminType"));
                schedule.setDay(DayOfWeek.valueOf(r.get("day").toUpperCase()));
                schedule.setTime(LocalTime.parse(r.get("time")));

                results.add(schedule);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read schedule CSV", e);
        }

        return results;
    }

    @Override
    public List<DispenseSchedule> findDue(Instant now) {

        // Interpret "now" in the system's default timezone because CSV times are stored
        // as local wall-clock times without explicit timezone information.
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        DayOfWeek today = nowLocal.getDayOfWeek();
        LocalTime currentTime = nowLocal.toLocalTime();

        DayOfWeek yesterday = today.minus(1);

        // only return schedules within a 5-minute window
        LocalTime fiveMinutesAgo = currentTime.minusMinutes(5);

        return findAll().stream()
                .filter(s -> {
                    LocalTime scheduleTime = s.getTime();

                    // Window does not cross midnight: simple same-day range
                    if (!fiveMinutesAgo.isAfter(currentTime)) {
                        return s.getDay().equals(today)
                                && !scheduleTime.isBefore(fiveMinutesAgo)
                                && !scheduleTime.isAfter(currentTime);
                    }

                    // Window crosses midnight:
                    // - include schedules on "yesterday" from fiveMinutesAgo up to midnight
                    // - include schedules on "today" from midnight up to currentTime
                    boolean isYesterdayInWindow = s.getDay().equals(yesterday)
                            && !scheduleTime.isBefore(fiveMinutesAgo);
                    boolean isTodayInWindow = s.getDay().equals(today)
                            && !scheduleTime.isAfter(currentTime);

                    return isYesterdayInWindow || isTodayInWindow;
                })
                .toList();
    }

    @Override
    public void saveAll(List<DispenseSchedule> schedules) {
        try {
            // Read existing entries
            List<DispenseSchedule> existing = findAll();

            // Find the highest existing ID
            int maxId = existing.stream()
                    .mapToInt(DispenseSchedule::getId)
                    .max()
                    .orElse(0);

            // Create a map of existing schedules by ID for easy lookup
            Map<Integer, DispenseSchedule> existingMap = existing.stream()
                    .collect(Collectors.toMap(DispenseSchedule::getId, s -> s));

            // Process incoming schedules
            for (DispenseSchedule schedule : schedules) {
                if (schedule.getId() == null || schedule.getId() == 0) {
                    // New schedule - assign new ID
                    schedule.setId(++maxId);
                }
                // Add/update in the map (this replaces if ID already exists)
                existingMap.put(schedule.getId(), schedule);
            }

            // Write all schedules back to CSV
            writeToCSV(new ArrayList<>(existingMap.values()));

        } catch (Exception e) {
            throw new IllegalStateException("Failed to save schedules to CSV", e);
        }
    }

    private void writeToCSV(List<DispenseSchedule> schedules) throws Exception {
        // Get the file path from the Resource
        File file = csv.getFile();

        try (FileWriter writer = new FileWriter(file);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("id", "numberOfPills", "vitaminType", "day", "time"))) {

            for (DispenseSchedule schedule : schedules) {
                printer.printRecord(
                        schedule.getId(),
                        schedule.getNumberOfPills(),
                        schedule.getVitaminType(),
                        schedule.getDay().name(),
                        schedule.getTime().toString()
                );
            }
        }
    }



}
