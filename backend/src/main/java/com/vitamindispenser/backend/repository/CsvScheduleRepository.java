package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import org.springframework.core.io.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.InputStreamReader;
import java.util.Optional;

@Repository
public class CsvScheduleRepository implements ScheduleRepository {

    private final Resource csv;

    public CsvScheduleRepository(
            @Value("${schedule.csv.location}") Resource csv
    ) {
        this.csv = csv;
    }

    @Override
    public Optional<DispenseEvent> findById(Long id) {
        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(csv.getInputStream()))) {

            for (CSVRecord r : parser) {
                if (Long.parseLong(r.get("id")) == id) {
                    return Optional.of(
                            new DispenseEvent(
                                    Integer.parseInt(r.get("numberOfPills")),
                                    r.get("vitaminType"),
                                    r.get("day"),
                                    r.get("time"),
                                    false,
                                    Integer.parseInt(r.get("id"))
                            )
                    );
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to read schedule CSV", e);
        }

        return Optional.empty();
    }
}
