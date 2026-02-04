package com.vitamindispenser.backend.domainTests;

import com.vitamindispenser.backend.domain.logging.LoggingExportService;
import com.vitamindispenser.backend.dto.logging.LoggingDatabase;
import com.vitamindispenser.backend.repository.DispenseEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingExportServiceTest {

    @Mock
    private DispenseEventLogRepository repo;

    @InjectMocks
    private LoggingExportService exportService;

    @Test
    void exportAllLogsAsCsv_whenNoRows_returnsHeaderOnly() {
        when(repo.findAll()).thenReturn(List.of());

        String csv = exportService.exportAllLogsAsCsv();

        assertThat(csv).isEqualTo("id,intakeId,vitaminType,day,time,numberOfPills,taken\n");
    }

    @Test
    void exportAllLogsAsCsv_withRows_outputsExpectedCsv() {
        LoggingDatabase r1 = new LoggingDatabase();
        r1.setLogId(100L);
        r1.setIntakeId(1);
        r1.setVitaminType("Vitamin C");
        r1.setDay("MONDAY");
        r1.setTime("08:00");
        r1.setNumberOfPills(2);
        r1.setTaken(true);

        LoggingDatabase r2 = new LoggingDatabase();
        r2.setLogId(101L);
        r2.setIntakeId(2);
        r2.setVitaminType("Iron");
        r2.setDay("TUESDAY");
        r2.setTime("12:00");
        r2.setNumberOfPills(1);
        r2.setTaken(false);

        when(repo.findAll()).thenReturn(List.of(r1, r2));

        String csv = exportService.exportAllLogsAsCsv();

        assertThat(csv).isEqualTo(
                "id,intakeId,vitaminType,day,time,numberOfPills,taken\n" +
                        "100,1,Vitamin C,MONDAY,08:00,2,true\n" +
                        "101,2,Iron,TUESDAY,12:00,1,false\n"
        );
    }

    @Test
    void exportAllLogsAsCsv_escapesCommasQuotesAndNewlines() {
        LoggingDatabase r = new LoggingDatabase();
        r.setLogId(200L);
        r.setIntakeId(3);
        r.setVitaminType("Vit, \"C\"\nPlus"); // comma + quotes + newline
        r.setDay("MONDAY");
        r.setTime("08:00");
        r.setNumberOfPills(2);
        r.setTaken(true);

        when(repo.findAll()).thenReturn(List.of(r));

        String csv = exportService.exportAllLogsAsCsv();

        // Quotes are doubled, and whole field is quoted
        assertThat(csv).isEqualTo(
                "id,intakeId,vitaminType,day,time,numberOfPills,taken\n" +
                        "200,3,\"Vit, \"\"C\"\"\nPlus\",MONDAY,08:00,2,true\n"
        );
    }

    @Test
    void exportAllLogsAsCsv_nullFields_becomeEmptyCells() {
        LoggingDatabase r = new LoggingDatabase();
        r.setLogId(null);
        r.setIntakeId(null);
        r.setVitaminType(null);
        r.setDay(null);
        r.setTime(null);
        r.setNumberOfPills(null);
        r.setTaken(null);

        when(repo.findAll()).thenReturn(List.of(r));

        String csv = exportService.exportAllLogsAsCsv();

        assertThat(csv).isEqualTo(
                "id,intakeId,vitaminType,day,time,numberOfPills,taken\n" +
                        ",,,,,,\n"
        );
    }
    @Test
    void exportAllLogsAsCsv_whenDatabaseIsEmpty_returnsHeaderOnly() {
        // given
        when(repo.findAll()).thenReturn(List.of());

        // when
        String csv = exportService.exportAllLogsAsCsv();

        // then
        assertThat(csv)
                .isEqualTo("id,intakeId,vitaminType,day,time,numberOfPills,taken\n");
    }

    @Test
    void exportRawDashboardCsv_whenDatabaseIsEmpty_returnsHeaderOnly() {
        when(repo.findAll()).thenReturn(List.of());

        String csv = exportService.exportDashboardCsv();

        assertThat(csv).isEqualTo("vitaminType,day,time,numberOfPills,taken\n");
    }

    @Test
    void exportRawDashboardCsv_withRows_outputsExpectedCsv_withoutIds() {
        LoggingDatabase r1 = new LoggingDatabase();
        r1.setLogId(100L);
        r1.setIntakeId(1);
        r1.setVitaminType("Vitamin C");
        r1.setDay("MONDAY");
        r1.setTime("08:00");
        r1.setNumberOfPills(2);
        r1.setTaken(true);

        LoggingDatabase r2 = new LoggingDatabase();
        r2.setLogId(101L);
        r2.setIntakeId(2);
        r2.setVitaminType("Iron");
        r2.setDay("TUESDAY");
        r2.setTime("12:00");
        r2.setNumberOfPills(1);
        r2.setTaken(false);

        when(repo.findAll()).thenReturn(List.of(r1, r2));

        String csv = exportService.exportDashboardCsv();

        // no id / intakeId columns
        assertThat(csv).isEqualTo(
                "vitaminType,day,time,numberOfPills,taken\n" +
                        "Vitamin C,MONDAY,08:00,2,true\n" +
                        "Iron,TUESDAY,12:00,1,false\n"
        );
    }

    @Test
    void exportRawDashboardCsv_escapesCommasQuotesAndNewlines() {
        LoggingDatabase r = new LoggingDatabase();
        r.setVitaminType("Vit, \"C\"\nPlus"); // comma + quotes + newline
        r.setDay("MONDAY");
        r.setTime("08:00");
        r.setNumberOfPills(2);
        r.setTaken(true);

        when(repo.findAll()).thenReturn(List.of(r));

        String csv = exportService.exportDashboardCsv();

        assertThat(csv).isEqualTo(
                "vitaminType,day,time,numberOfPills,taken\n" +
                        "\"Vit, \"\"C\"\"\nPlus\",MONDAY,08:00,2,true\n"
        );
    }

    @Test
    void exportRawDashboardCsv_nullFields_becomeEmptyCells() {
        LoggingDatabase r = new LoggingDatabase();
        r.setVitaminType(null);
        r.setDay(null);
        r.setTime(null);
        r.setNumberOfPills(null);
        r.setTaken(null);

        when(repo.findAll()).thenReturn(List.of(r));

        String csv = exportService.exportDashboardCsv();

        assertThat(csv).isEqualTo(
                "vitaminType,day,time,numberOfPills,taken\n" +
                        ",,,,\n"
        );
    }
}
