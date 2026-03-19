package com.vitamindispenser.backend.firmware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitamindispenser.backend.firmware.dto.PollCommandResult;
import com.vitamindispenser.backend.logging.LoggingService;
import com.vitamindispenser.backend.schedule.ScheduleEntry;
import com.vitamindispenser.backend.schedule.Slot;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import com.vitamindispenser.backend.schedule.SlotRepository;
import com.vitamindispenser.backend.user.UserRepository;
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
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final LoggingService loggingService;

    private volatile String pendingCommand;
    private final Map<Integer, Long> lastDispenseSentAt = new ConcurrentHashMap<>();

    public PollCommandService(ScheduleEntryRepository scheduleEntryRepository,
                              SlotRepository slotRepository,
                              UserRepository userRepository,
                              LoggingService loggingService) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
    }

    /**
     * calculates what command to send to the Arduino based on the user's schedule and paused status.
     * the command returned is either DISPENSE, IDLE, ADVANCE or SNOOZE
     * @param user the user whose schedule is being polled
     * @return the PollCommandResult to send the Arduino, including the command, intake ids and physical slot
     */
    public PollCommandResult getPollingResults(User user) {
        if (user.isPaused()) {
            return new PollCommandResult("IDLE", Collections.emptyList(), null);
        }

        if (user.getSlotsToAdvance() > 0) {
            user.setSlotsToAdvance(user.getSlotsToAdvance() - 1);

            try {
                ObjectMapper mapper = new ObjectMapper();
                List<String> queue = new ArrayList<>(List.of(
                        mapper.readValue(user.getMissedSlotQueue() != null ? user.getMissedSlotQueue() : "[]", String[].class)
                ));

                if (!queue.isEmpty()) {
                    String entry = queue.remove(0);
                    String[] parts = entry.split("\\|");
                    String day = parts[0];
                    String time = parts[1];
                    String type = parts.length > 2 ? parts[2] : "ADVANCE";

                    List<ScheduleEntry> scheduleEntries = scheduleEntryRepository
                            .findByUserAndDayAndTime(user, day, time);

                    if (!scheduleEntries.isEmpty()) {
                        List<Integer> ids = scheduleEntries.stream()
                                .map(ScheduleEntry::getId)
                                .toList();

                        if ("DISPENSE".equals(type)) {
                            user.setMissedSlotQueue(mapper.writeValueAsString(queue));
                            userRepository.save(user);
                            Integer slotNumber = slotRepository
                                    .findByUserOrderBySlotNumber(user)
                                    .stream()
                                    .filter(s -> day.equals(s.getAssignedDay()) && time.equals(s.getAssignedTime()))
                                    .findFirst()
                                    .map(Slot::getSlotNumber)
                                    .orElse(null);
                            return new PollCommandResult("DISPENSE", ids, slotNumber);
                        } else {
                            loggingService.handleStatus(ids, false, user);
                            System.out.println("MISSED SLOT DETECTED: " + day + " " + time);
                            System.out.println("IDS: " + ids);
                        }
                    }

                    user.setMissedSlotQueue(mapper.writeValueAsString(queue));
                }

                userRepository.save(user);
                return new PollCommandResult("ADVANCE", Collections.emptyList(), null);

            } catch (Exception e) {
                System.out.println("Failed to process slot queue: " + e.getMessage());
                userRepository.save(user);
                return new PollCommandResult("ADVANCE", Collections.emptyList(), null);
            }
        }

        if (pendingCommand != null) {
            String cmd = pendingCommand;
            pendingCommand = null;
            return new PollCommandResult(cmd, Collections.emptyList(), null);
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
            Integer slotNumber = entries.stream()
                    .filter(e -> intakeIds.contains(e.getId()))
                    .findFirst()
                    .flatMap(e -> slotRepository
                            .findByUserOrderBySlotNumber(user)
                            .stream()
                            .filter(s -> e.getDay().equals(s.getAssignedDay())
                                    && e.getTime().equals(s.getAssignedTime()))
                            .findFirst()
                            .map(Slot::getSlotNumber))
                    .orElse(null);
            return new PollCommandResult("DISPENSE", intakeIds, slotNumber);
        }

        return new PollCommandResult("IDLE", Collections.emptyList(), null);
    }

    /**
     * sets a pending one-shot command to be sent to the Arduino on the next poll
     * @param command the command to set — only SNOOZE is permitted from the app
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