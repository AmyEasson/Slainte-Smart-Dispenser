package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
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
    public List<DispenseEvent> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<DispenseEvent> results = new ArrayList<>();

        Set<Integer> idSet = new HashSet<>(ids);

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(csv.getInputStream()))) {

            for (CSVRecord r : parser) {

                Integer rowId = Integer.parseInt(r.get("id"));

                if (!idSet.contains(rowId)) {
                    continue;
                }

                results.add(
                        new DispenseEvent(
                                Integer.parseInt(r.get("numberOfPills")),
                                r.get("vitaminType"),
                                r.get("day"),
                                r.get("time"),
                                false,
                                rowId
                        )
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to read schedule CSV", e);
        }

        return results;
    }

    @Override
    public List<DispenseEvent> findAll() {
        List<DispenseEvent> results = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(csv.getInputStream()))) {

            for (CSVRecord r : parser) {
                results.add(
                        new DispenseEvent(
                                Integer.parseInt(r.get("numberOfPills")),
                                r.get("vitaminType"),
                                r.get("day"),
                                r.get("time"),
                                false,  // not taken yet
                                Integer.parseInt(r.get("id"))
                        )
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read schedule CSV", e);
        }

        return results;
    }

    @Override
    public void saveAll(List<DispenseEvent> events) {
        try {
            // Read existing entries first
            List<DispenseEvent> existing = findAll();

            // Find the highest existing ID
            int maxId = existing.stream()
                    .mapToInt(DispenseEvent::getId)
                    .max()
                    .orElse(0);

            // Assign new IDs to events that don't have one
            for (DispenseEvent event : events) {
                if (event.getId() == null || event.getId() == 0) {
                    event.setId(++maxId);
                }
            }

            // Combine existing + new events
            List<DispenseEvent> allEvents = new ArrayList<>(existing);
            allEvents.addAll(events);

            // Write all back to CSV
            writeToCSV(allEvents);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to save schedules to CSV", e);
        }
    }

    private void writeToCSV(List<DispenseEvent> events) throws Exception {
        // Get the file path from the Resource
        File file = csv.getFile();

        try (FileWriter writer = new FileWriter(file);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("id", "numberOfPills", "vitaminType", "day", "time"))) {

            for (DispenseEvent event : events) {
                printer.printRecord(
                        event.getId(),
                        event.getNumberOfPills(),
                        event.getVitaminType(),
                        event.getDay(),
                        event.getTime()
                );
            }
        }
    }



}
