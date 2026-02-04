package com.vitamindispenser.backend.domain.logging;

import com.vitamindispenser.backend.dto.logging.LoggingDatabase;
import com.vitamindispenser.backend.repository.DispenseEventLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoggingExportService {

    private final DispenseEventLogRepository repo;

    public LoggingExportService(DispenseEventLogRepository repo) {
        this.repo = repo;
    }

    /** Full export (with IDs) – used by /logs/export.csv */
    public String exportAllLogsAsCsv() {
        List<LoggingDatabase> rows = repo.findAll();

        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append("id,intakeId,vitaminType,day,time,numberOfPills,taken\n");

        for (LoggingDatabase r : rows) {
            sb.append(csv(r.getLogId())).append(',')
                    .append(csv(r.getIntakeId())).append(',')
                    .append(csv(r.getVitaminType())).append(',')
                    .append(csv(r.getDay())).append(',')
                    .append(csv(r.getTime())).append(',')
                    .append(csv(r.getNumberOfPills())).append(',')
                    .append(csv(r.getTaken()))
                    .append('\n');
        }

        return sb.toString();
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);

        boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        return mustQuote ? "\"" + s + "\"" : s;
    }
    public boolean hasAnyLogs() {
        return repo.count() > 0;
    }

    /** RAW dashboard export (NO IDs) – used by /intake */
    public String exportDashboardCsv() {
        List<LoggingDatabase> rows = repo.findAll();

        StringBuilder sb = new StringBuilder();
        sb.append("vitaminType,day,time,numberOfPills,taken\n");

        for (LoggingDatabase r : rows) {
            sb.append(csv(r.getVitaminType())).append(',')
                    .append(csv(r.getDay())).append(',')
                    .append(csv(r.getTime())).append(',')
                    .append(csv(r.getNumberOfPills())).append(',')
                    .append(csv(r.getTaken()))
                    .append('\n');
        }

        return sb.toString();
    }
}

