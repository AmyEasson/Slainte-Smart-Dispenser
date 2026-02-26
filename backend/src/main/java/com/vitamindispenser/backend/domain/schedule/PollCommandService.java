package com.vitamindispenser.backend.domain.schedule;

import com.vitamindispenser.backend.dto.firmware.PollCommandResult;
import com.vitamindispenser.backend.model.ScheduleEntry;
import com.vitamindispenser.backend.model.User;
import com.vitamindispenser.backend.repository.ScheduleEntryRepository;
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

    private final ScheduleEntryRepository scheduleEntryRepository;

    private volatile String pendingCommand;
    private final Map<Integer, Long> lastDispenseSentAt = new ConcurrentHashMap<>();

    public PollCommandService(ScheduleEntryRepository scheduleEntryRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    public PollCommandResult getPollingResults(User user) {
        if (pendingCommand != null) {
            String cmd = pendingCommand;
            pendingCommand = null;
            return new PollCommandResult(cmd, Collections.emptyList());
        }

        String today = DayOfWeek.from(java.time.LocalDate.now())
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toLowerCase(Locale.ENGLISH);
        String nowTime = LocalTime.now().format(TIME_FORMAT);

        List<ScheduleEntry> entries = scheduleEntryRepository.findByUser(user);
        long nowMs = System.currentTimeMillis();
        List<Integer> intakeIds = new ArrayList<>();

        for (ScheduleEntry e : entries) {
            if (!e.getDay().equalsIgnoreCase(today)) continue;
            if (!e.getTime().equals(nowTime)) continue;

            Long last = lastDispenseSentAt.get(e.getId());
            if (last != null && (nowMs - last) < DISPENSE_COOLDOWN_MINUTES * 60 * 1000) {
                continue;
            }
            lastDispenseSentAt.put(e.getId(), nowMs);
            intakeIds.add(e.getId());
        }

        if (!intakeIds.isEmpty()) {
            return new PollCommandResult("DISPENSE", intakeIds);
        }
        return new PollCommandResult("IDLE", Collections.emptyList());
    }

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
