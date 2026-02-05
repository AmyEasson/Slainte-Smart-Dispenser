package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.Log;
import com.vitamindispenser.backend.dto.schedule.DispenseEvent;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Repository
public class CsvScheduleRepository implements ScheduleRepository {

    private final Resource csv;

    public CsvScheduleRepository(
            @Value("${schedule.csv.location}") Resource csv
    ) {
        this.csv = csv;
    }

    /**
     * Ensuring the csv file actually exists, creating it if not.
     */
    @PostConstruct
    void ensureCsvExists() {
        try {
            File file = csv.getFile();
            File parent = file.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("id,vitaminType,numberOfPills,day,time\n");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize schedule CSV", e);
        }
    }


    /*
    This method is important to enable logging.
    Relevant information from the scheduling database need to be fetched in order to be logged along with the status.
     */
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
    public List<DispenseEvent> saveAll(List<DispenseEvent> events) {
        if (events == null || events.isEmpty()) {
            // still overwrite with empty file
            try {
                writeEventsToCsv(Collections.emptyList());
                return Collections.emptyList();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to clear schedule CSV", e);
            }
        }

        try {
            int nextId = 1;

            List<DispenseEvent> eventsWithIds = new ArrayList<>();
            for (DispenseEvent event : events) {
                DispenseEvent eventWithId = new DispenseEvent(
                        event.getNumberOfPills(),
                        event.getVitaminType(),
                        event.getDay(),
                        event.getTime(),
                        nextId++
                );
                eventsWithIds.add(eventWithId);
            }

            // overwrite file with ONLY these events
            writeEventsToCsv(eventsWithIds);

            return eventsWithIds;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to write schedule CSV", e);
        }
    }


    private void writeEventsToCsv(List<DispenseEvent> events) throws IOException {
        File file = csv.getFile();
        File tempFile = new File(file.getParent(), file.getName() + ".tmp");

        try (FileWriter writer = new FileWriter(tempFile);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            printer.printRecord("id", "vitaminType", "numberOfPills", "day", "time");


            for (DispenseEvent event : events) {
                printer.printRecord(
                        event.getId(),
                        event.getVitaminType(),
                        event.getNumberOfPills(),
                        event.getDay(),
                        event.getTime()
                );
            }
        }

        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }


}
