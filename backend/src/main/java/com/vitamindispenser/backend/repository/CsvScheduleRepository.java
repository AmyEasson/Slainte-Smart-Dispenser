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

        LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        DayOfWeek today = nowUtc.getDayOfWeek();
        LocalTime currentTime = nowUtc.toLocalTime();

        // only return schedules within a 5-minute window
        LocalTime fiveMinutesAgo = currentTime.minusMinutes(5);

        return findAll().stream()
                .filter(s ->
                        s.getDay().equals(today) &&
                                // schedule time is between 5 minutes ago and now
                                !s.getTime().isBefore(fiveMinutesAgo) &&
                                !s.getTime().isAfter(currentTime)
                )
                .toList();
    }


    @Override
    public void saveAll(List<DispenseSchedule> schedules) {
        try {
            // Read existing entries first
            List<DispenseSchedule> existing = findAll();

            // Find the highest existing ID
            int maxId = existing.stream()
                    .mapToInt(DispenseSchedule::getId)
                    .max()
                    .orElse(0);

            // Assign new IDs to events that don't have one
            for (DispenseSchedule schedule : schedules) {
                if (schedule.getId() == null || schedule.getId() == 0) {
                    schedule.setId(++maxId);
                }
            }

            // Combine existing + new events
            List<DispenseSchedule> allEvents = new ArrayList<>(existing);
            allEvents.addAll(schedules);

            // Write all back to CSV
            writeToCSV(allEvents);

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
