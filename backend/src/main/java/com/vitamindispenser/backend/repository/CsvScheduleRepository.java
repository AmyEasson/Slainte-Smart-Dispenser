package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import org.springframework.core.io.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

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

}
