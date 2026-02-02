package com.vitamindispenser.backend.domain;

import com.vitamindispenser.backend.dto.logging.DispenseEvent;
import com.vitamindispenser.backend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which command (IDLE, DISPENSE, SNOOZE) to send to the firmware when it polls.
 * DISPENSE only when current day+time matches a schedule slot; SNOOZE when app requested it; else IDLE.
 * The mobile app may only set SNOOZE, not DISPENSE.
 */
@Service
public class PollCommandService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final long DISPENSE_COOLDOWN_MINUTES = 5;

    private final ScheduleRepository scheduleRepository;

    /** App-set override: return this once on next poll, then clear. */
    private volatile String pendingCommand;

    /** Slot id -> time we last sent DISPENSE for it (ms), so we don't dispense every 2s for 2 min. */
    private final Map<Integer, Long> lastDispenseSentAt = new ConcurrentHashMap<>();

    public PollCommandService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Returns the command and, when DISPENSE, the intake/slot ids for this dispense.
     */
    public PollCommandResult getPollingResults() {
        if (pendingCommand != null) {
            String cmd = pendingCommand;
            pendingCommand = null;
            return new PollCommandResult(cmd, Collections.emptyList());
        }

        String today = DayOfWeek.from(java.time.LocalDate.now())
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toLowerCase(Locale.ENGLISH);
        String nowTime = LocalTime.now().format(TIME_FORMAT);

        List<DispenseEvent> all = scheduleRepository.findAll();
        long nowMs = System.currentTimeMillis();
        List<Integer> intakeIds = new ArrayList<>();

        for (DispenseEvent e : all) {
            if (!e.getDay().equalsIgnoreCase(today)) continue;
            if (!e.getTime().equals(nowTime)) continue;

            Long last = lastDispenseSentAt.get(e.getId());
            if (last != null && (nowMs - last) < DISPENSE_COOLDOWN_MINUTES * 60 * 1000) {
                continue; // already sent DISPENSE for this slot recently
            }
            lastDispenseSentAt.put(e.getId(), nowMs);
            intakeIds.add(e.getId());
        }

        if (!intakeIds.isEmpty()) {
            return new PollCommandResult("DISPENSE", intakeIds);
        }
        return new PollCommandResult("IDLE", Collections.emptyList());
    }

    /**
     * Sets a one-shot SNOOZE for the next poll. Only SNOOZE is accepted from the mobile app;
     * DISPENSE is controlled solely by the schedule.
     */
    public void setPendingCommand(String command) {
        if (command == null || command.isBlank() || "IDLE".equalsIgnoreCase(command) || "DISPENSE".equalsIgnoreCase(command)) {
            pendingCommand = null;
            return;
        }
        if ("SNOOZE".equalsIgnoreCase(command)) {
            pendingCommand = "SNOOZE";
        }
    }
}
