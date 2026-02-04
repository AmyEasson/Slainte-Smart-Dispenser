package com.vitamindispenser.backend.domainTests;

import com.vitamindispenser.backend.domain.exceptions.ScheduleNotFoundException;
import com.vitamindispenser.backend.domain.logging.LoggingService;
import com.vitamindispenser.backend.dto.logging.Log;
import com.vitamindispenser.backend.dto.logging.LoggingDatabase;
import com.vitamindispenser.backend.dto.schedule.DispenseEvent;
import com.vitamindispenser.backend.repository.DispenseEventLogRepository;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private DispenseEventLogRepository logRepository;

    @InjectMocks
    private LoggingService loggingService;

    @BeforeEach
    void setUp() {
    }

    /*
    *  handleStatus method tests:
    * */
    @Test
    void handleStatus_takenTrue_singleEvent() {
        // given
        List<Integer> intakeIds = List.of(1);
        DispenseEvent event =
                new DispenseEvent(2, "Vitamin C", "MONDAY", "08:00", 1);

        when(scheduleRepository.findByIds(intakeIds))
                .thenReturn(List.of(event));

        ArgumentCaptor<List<LoggingDatabase>> captor =
                ArgumentCaptor.forClass(List.class);

        // when
        loggingService.handleStatus(intakeIds, true);

        // then
        verify(scheduleRepository).findByIds(intakeIds);
        verify(logRepository).saveAll(captor.capture());

        List<LoggingDatabase> saved = captor.getValue();
        assertThat(saved).hasSize(1);

        LoggingDatabase row = saved.get(0);
        assertThat(row.getIntakeId()).isEqualTo(1);
        assertThat(row.getVitaminType()).isEqualTo("Vitamin C");
        assertThat(row.getDay()).isEqualTo("MONDAY");
        assertThat(row.getTime()).isEqualTo("08:00");
        assertThat(row.getNumberOfPills()).isEqualTo(2);
        assertThat(row.getTaken()).isTrue();
    }


    @Test
    void handleStatus_takenFalse_multipleEvents() {
        // given
        List<Integer> intakeIds = List.of(10, 20);

        DispenseEvent e1 =
                new DispenseEvent(1, "Vitamin D", "TUESDAY", "09:00", 10);
        DispenseEvent e2 =
                new DispenseEvent(3, "Iron", "TUESDAY", "21:30", 20);

        when(scheduleRepository.findByIds(intakeIds))
                .thenReturn(List.of(e1, e2));

        ArgumentCaptor<List<LoggingDatabase>> captor =
                ArgumentCaptor.forClass(List.class);

        // when
        loggingService.handleStatus(intakeIds, false);

        // then
        verify(logRepository).saveAll(captor.capture());

        List<LoggingDatabase> saved = captor.getValue();
        assertThat(saved).hasSize(2);

        LoggingDatabase first = saved.get(0);
        assertThat(first.getIntakeId()).isEqualTo(10);
        assertThat(first.getVitaminType()).isEqualTo("Vitamin D");
        assertThat(first.getNumberOfPills()).isEqualTo(1);
        assertThat(first.getTaken()).isFalse();

        LoggingDatabase second = saved.get(1);
        assertThat(second.getIntakeId()).isEqualTo(20);
        assertThat(second.getVitaminType()).isEqualTo("Iron");
        assertThat(second.getNumberOfPills()).isEqualTo(3);
        assertThat(second.getTaken()).isFalse();
    }

    @Test
    void handleStatus_whenScheduleReturnsEmpty_shouldThrowAndNotSave() {
        List<Integer> intakeIds = List.of(1, 2, 3);

        when(scheduleRepository.findByIds(intakeIds)).thenReturn(List.of());

        assertThatThrownBy(() -> loggingService.handleStatus(intakeIds, true))
                .isInstanceOf(ScheduleNotFoundException.class);

        verify(logRepository, never()).saveAll(any());
    }


    @Test
    void handleStatus_nullIntakeIds_throwsException() {
        assertThatThrownBy(() ->
                loggingService.handleStatus(null, true)
        ).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(scheduleRepository, logRepository);
    }

    @Test
    void handleStatus_emptyListIntakeIds_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                loggingService.handleStatus(List.of(), true)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intakeIds must not be empty");

        verifyNoInteractions(scheduleRepository, logRepository);
    }

    @Test
    void handleStatus_nullStatus_throwsException() {
        assertThatThrownBy(() ->
                loggingService.handleStatus(List.of(1), null)
        ).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(scheduleRepository, logRepository);
    }

    /*
     *  fromDispenseEvent method tests:
     *
     * */
    @Test
    void fromDispenseEvent_nullEvent_returnsNull() {
        assertThat(loggingService.fromDispenseEvent(null)).isNull();
    }

    @Test
    void fromDispenseEvent_mapsAllFields_andDefaultsTakenFalse() {
        // given
        DispenseEvent event = new DispenseEvent(3, "Zinc", "WEDNESDAY", "07:15", 42);

        // when
        Log log = loggingService.fromDispenseEvent(event);

        // then
        assertThat(log).isNotNull();
        assertThat(log.getVitaminType()).isEqualTo("Zinc");
        assertThat(log.getDay()).isEqualTo("WEDNESDAY");
        assertThat(log.getTime()).isEqualTo("07:15");
        assertThat(log.getNumberOfPills()).isEqualTo(3);
        assertThat(log.getId()).isEqualTo(42);
        assertThat(log.getTaken()).isFalse();
    }

    @Test
    void fromDispenseEvent_allowsNullFields_andCopiesThem() {
        // given
        DispenseEvent event = new DispenseEvent();
        event.setVitaminType(null);
        event.setDay(null);
        event.setTime(null);
        event.setNumberOfPills(null);
        event.setId(null);

        // when
        Log log = loggingService.fromDispenseEvent(event);

        // then
        assertThat(log).isNotNull();
        assertThat(log.getVitaminType()).isNull();
        assertThat(log.getDay()).isNull();
        assertThat(log.getTime()).isNull();
        assertThat(log.getNumberOfPills()).isNull();
        assertThat(log.getId()).isNull();
        assertThat(log.getTaken()).isFalse(); // still defaulted
    }

    /*
     *  logEvents method tests:
     *
     * */

    @Test
    void logEvents_mapsLogsToDatabaseRows_andSavesAll() {
        // given
        Log l1 = new Log();
        l1.setId(1);
        l1.setVitaminType("Vitamin C");
        l1.setDay("MONDAY");
        l1.setTime("08:00");
        l1.setNumberOfPills(2);
        l1.setTaken(true);

        Log l2 = new Log();
        l2.setId(2);
        l2.setVitaminType("Iron");
        l2.setDay("TUESDAY");
        l2.setTime("12:00");
        l2.setNumberOfPills(1);
        l2.setTaken(false);

        ArgumentCaptor<List<LoggingDatabase>> captor = ArgumentCaptor.forClass(List.class);

        // when
        loggingService.logEvents(List.of(l1, l2));

        // then
        verify(logRepository).saveAll(captor.capture());
        List<LoggingDatabase> saved = captor.getValue();

        assertThat(saved).hasSize(2);

        LoggingDatabase r1 = saved.get(0);
        assertThat(r1.getIntakeId()).isEqualTo(1);
        assertThat(r1.getVitaminType()).isEqualTo("Vitamin C");
        assertThat(r1.getDay()).isEqualTo("MONDAY");
        assertThat(r1.getTime()).isEqualTo("08:00");
        assertThat(r1.getNumberOfPills()).isEqualTo(2);
        assertThat(r1.getTaken()).isTrue();

        LoggingDatabase r2 = saved.get(1);
        assertThat(r2.getIntakeId()).isEqualTo(2);
        assertThat(r2.getVitaminType()).isEqualTo("Iron");
        assertThat(r2.getDay()).isEqualTo("TUESDAY");
        assertThat(r2.getTime()).isEqualTo("12:00");
        assertThat(r2.getNumberOfPills()).isEqualTo(1);
        assertThat(r2.getTaken()).isFalse();
    }

    @Test
    void logEvents_emptyList_savesEmptyList() {
        // given
        ArgumentCaptor<List<LoggingDatabase>> captor = ArgumentCaptor.forClass(List.class);

        // when
        loggingService.logEvents(List.of());

        // then
        verify(logRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

}

