package com.vitamindispenser.backend.logging;

import com.vitamindispenser.backend.logging.dto.IntakeForRawDashboard;
import com.vitamindispenser.backend.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoggingExportService {

    private final DispenseEventLogRepository repo;

    public LoggingExportService(DispenseEventLogRepository repo) {
        this.repo = repo;
    }

    /** Full export (with IDs) – used by /logs/export.csv */
    public String exportAllLogsAsCsv(User user) {
        List<LoggingDatabase> rows = repo.findByUser(user);

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

    /** RAW dashboard export (NO IDs) – used by /intake */
    public List<IntakeForRawDashboard> exportDashboardJson(User user) {
        return repo.findByUser(user).stream()
                .map(r -> new IntakeForRawDashboard(
                        r.getVitaminType(),
                        r.getDate().toString(),
                        r.getDay(),
                        r.getTime(),
                        r.getNumberOfPills(),
                        r.getTaken()
                ))
                .toList();
    }
}

