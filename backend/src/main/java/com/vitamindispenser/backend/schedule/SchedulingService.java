package com.vitamindispenser.backend.schedule;

import com.vitamindispenser.backend.schedule.dto.DaySchedule;
import com.vitamindispenser.backend.schedule.dto.ScheduleRequest;
import com.vitamindispenser.backend.schedule.dto.VitaminSchedule;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SchedulingService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Autowired
    public SchedulingService(ScheduleEntryRepository scheduleEntryRepository,
                             SlotRepository slotRepository,
                             UserRepository userRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    /**
     * saves the user's new schedule, overwriting the old one with all changes
     * @param request the user's submitted schedule information
     * @param user the User entity whose entry is being updated
     */
    @Transactional
    public void saveSchedule(ScheduleRequest request, User user) {

        scheduleEntryRepository.deleteByUser(user);
        slotRepository.deleteByUser(user);

        user.setFillCycleOffset(0);

        if (user.getLastFillDate() != null) {
            user.setScheduleChanged(true);
        }

        userRepository.save(user);

        List<ScheduleEntry> entries = new ArrayList<>();

        for (VitaminSchedule vitamin : request.getVitamins()) {
            for (DaySchedule daySchedule : vitamin.getSchedule()) {
                for (String time : daySchedule.getTimes()) {

                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setVitaminType(vitamin.getVitaminType());
                    entry.setNumberOfPills(vitamin.getNumberOfPills());
                    entry.setDay(daySchedule.getDay());
                    entry.setTime(time);
                    entry.setUser(user);

                    entries.add(entry);
                }
            }
        }

        initialiseSlots(user);

        scheduleEntryRepository.saveAll(entries);

        if (!entries.isEmpty()) {
            assignSlots(entries, user);
        }
    }

    /**
     * fetches the specified user's currently stored schedule
     * @param user the User entity whose schedule is to be accessed
     * @return the user's stored schedule
     */
    public ScheduleRequest retrieveSchedule(User user) {

        List<ScheduleEntry> entries = scheduleEntryRepository.findByUser(user);

        Map<String, VitaminSchedule> vitaminMap = new HashMap<>();

        for (ScheduleEntry entry : entries) {

            String vitaminKey = entry.getVitaminType() + "-" + entry.getNumberOfPills();

            VitaminSchedule vitaminSchedule =
                    vitaminMap.computeIfAbsent(vitaminKey, k -> {
                        VitaminSchedule v = new VitaminSchedule();
                        v.setVitaminType(entry.getVitaminType());
                        v.setNumberOfPills(entry.getNumberOfPills());
                        v.setSchedule(new ArrayList<>());
                        return v;
                    });

            DaySchedule daySchedule = vitaminSchedule.getSchedule().stream()
                    .filter(d -> d.getDay().equals(entry.getDay()))
                    .findFirst()
                    .orElseGet(() -> {
                        DaySchedule d = new DaySchedule();
                        d.setDay(entry.getDay());
                        d.setTimes(new ArrayList<>());
                        vitaminSchedule.getSchedule().add(d);
                        return d;
                    });

            daySchedule.getTimes().add(entry.getTime());
        }

        ScheduleRequest request = new ScheduleRequest();
        request.setVitamins(new ArrayList<>(vitaminMap.values()));

        return request;
    }

    /**
     * creates a slot entry for all 14 vitamin slots on the dispenser, and
     * marks slot 15 as reserved (must be kept empty)
     * @param user the User's whose dispenser slots are being initialised
     */
    private void initialiseSlots(User user) {

        List<Slot> slots = new ArrayList<>();

        for (int i = 1; i <= 15; i++) {

            Slot slot = new Slot();
            slot.setSlotNumber(i);
            slot.setReserved(i == 15);
            slot.setUser(user);

            slots.add(slot);
        }

        slotRepository.saveAll(slots);
    }

    /**
     * assigns vitamins to physical slots in the dispenser by grouping them by day and time
     * @param entries the list of all the user's supplements and their timings
     * @param user the user whose dispenser slots are being assigned
     */
    private void assignSlots(List<ScheduleEntry> entries, User user) {

        List<Slot> slots = slotRepository.findByUserOrderBySlotNumber(user)
                .stream()
                .filter(s -> !s.getReserved())
                .toList();

        List<String[]> uniqueTimes = entries.stream()
                .map(e -> e.getDay() + "|" + e.getTime())
                .distinct()
                .map(s -> s.split("\\|"))
                .toList();

        if (uniqueTimes.isEmpty()) {
            return;
        }

        int offset = user.getFillCycleOffset() % uniqueTimes.size();

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        Map<String, Integer> dayOrder = Map.of(
                "monday", 0,
                "tuesday", 1,
                "wednesday", 2,
                "thursday", 3,
                "friday", 4,
                "saturday", 5,
                "sunday", 6
        );

        int todayIndex = today.getValue() - 1;

        List<String[]> sorted = uniqueTimes.stream()
                .sorted((a, b) -> {

                    int dayA = dayOrder.get(a[0]);
                    int dayB = dayOrder.get(b[0]);

                    LocalTime timeA = LocalTime.parse(a[1]);
                    LocalTime timeB = LocalTime.parse(b[1]);

                    int minutesA =
                            ((dayA - todayIndex + 7) % 7) * 24 * 60
                                    + timeA.toSecondOfDay() / 60;

                    int minutesB =
                            ((dayB - todayIndex + 7) % 7) * 24 * 60
                                    + timeB.toSecondOfDay() / 60;

                    if (dayA == todayIndex && timeA.isBefore(now)) {
                        minutesA += 7 * 24 * 60;
                    }

                    if (dayB == todayIndex && timeB.isBefore(now)) {
                        minutesB += 7 * 24 * 60;
                    }

                    return Integer.compare(minutesA, minutesB);
                })
                .toList();

        slots.forEach(s -> {
            s.setAssignedDay(null);
            s.setAssignedTime(null);
        });

        for (int i = 0; i < 14; i++) {

            String[] dayTime = sorted.get((offset + i) % sorted.size());

            slots.get(i).setAssignedDay(dayTime[0]);
            slots.get(i).setAssignedTime(dayTime[1]);
        }

        slotRepository.saveAll(slots);
    }

    /**
     * fetches what vitamins should be in each physical dispenser slot
     * @param user the user whose information is to be updated
     * @return a map of which vitamins are in each dispenser slot
     */
    public Map<String, Object> getSlots(User user) {

        List<Slot> slots = slotRepository.findByUserOrderBySlotNumber(user);

        List<Map<String, Object>> slotList = slots.stream().map(slot -> {

            Map<String, Object> slotMap = new LinkedHashMap<>();

            slotMap.put("slotNumber", slot.getSlotNumber());
            slotMap.put("reserved", slot.getReserved());
            slotMap.put("assignedDay", slot.getAssignedDay());
            slotMap.put("assignedTime", slot.getAssignedTime());

            List<Map<String, Object>> vitamins =
                    (slot.getAssignedDay() == null)
                            ? List.of()
                            : scheduleEntryRepository
                            .findByUserAndDayAndTime(user,
                                    slot.getAssignedDay(),
                                    slot.getAssignedTime())
                            .stream()
                            .map(e -> {

                                Map<String, Object> v = new LinkedHashMap<>();
                                v.put("vitaminType", e.getVitaminType());
                                v.put("numberOfPills", e.getNumberOfPills());

                                return v;

                            }).toList();

            slotMap.put("vitamins", vitamins);

            return slotMap;

        }).toList();

        return Map.of("slots", slotList);
    }

    /**
     * calculates when the user should next refill the dispenser based on their schedule
     * and last refill date
     * @param user the user whose info is being updated
     * @return a map of any current warnings and the user's refill information
     */
    public Map<String, Object> getRefillInfo(User user) {

        List<ScheduleEntry> entries = scheduleEntryRepository.findByUser(user);

        Map<String, Object> result = new LinkedHashMap<>();

        if (entries.isEmpty()) {

            result.put("lastFillDate", null);
            result.put("refillDate", null);
            result.put("daysUntilRefill", null);
            result.put("warning", "NO_SCHEDULE");

            return result;
        }

        long uniqueTimes = entries.stream()
                .map(e -> e.getDay() + "|" + e.getTime())
                .distinct()
                .count();

        LocalDate lastFillDate = user.getLastFillDate();

        if (lastFillDate == null) {
            lastFillDate = LocalDate.now();
        }

        long daysUntilRefill = (long) Math.ceil((14.0 / uniqueTimes) * 7);

        LocalDate refillDate = lastFillDate.plusDays(daysUntilRefill);

        result.put("lastFillDate", lastFillDate.toString());
        result.put("refillDate", refillDate.toString());

        result.put("daysUntilRefill",
                ChronoUnit.DAYS.between(LocalDate.now(), refillDate));

        result.put("warning",
                user.isScheduleChanged() ? "SCHEDULE_CHANGED" : null);

        return result;
    }

    /**
     * resets last fill date to now, calls the assign slots function to represent
     * what pills are now physically in which slots of the dispenser
     * @param user the user who has filled their dispenser
     * @return the new refill information
     */
    public Map<String, Object> confirmFill(User user) {

        List<ScheduleEntry> entries = scheduleEntryRepository.findByUser(user);

        long totalUniqueTimes = entries.stream()
                .map(e -> e.getDay() + "|" + e.getTime())
                .distinct()
                .count();

        if (totalUniqueTimes > 0) {

            int newOffset =
                    (int) ((user.getFillCycleOffset() + 14) % totalUniqueTimes);

            user.setFillCycleOffset(newOffset);
        }

        user.setLastFillDate(LocalDate.now());
        user.setScheduleChanged(false);

        userRepository.save(user);

        assignSlots(entries, user);

        return getRefillInfo(user);
    }

    /**
     * calculates how many scheduled dispenses the user has missed while they have paused dispensing
     * @param user the user who paused their schedule
     * @param pausedAt the timestamp of when the user changed their schedule
     * @return how many scheduled dispenses were missed
     */
    public List<String> calculateMissedSlotQueue(User user, LocalDateTime pausedAt) {
        if (pausedAt == null) return List.of();

        LocalDateTime now = LocalDateTime.now();
        List<ScheduleEntry> entries = scheduleEntryRepository.findByUser(user);

        List<String> missed = new ArrayList<>();

        entries.stream()
                .map(e -> e.getDay() + "|" + e.getTime())
                .distinct()
                .forEach(dayTime -> {
                    String[] parts = dayTime.split("\\|");
                    String day = parts[0];
                    LocalTime time = LocalTime.parse(parts[1]);
                    DayOfWeek targetDay = DayOfWeek.valueOf(day.toUpperCase());

                    LocalDateTime cursor = pausedAt.toLocalDate().atTime(time);
                    while (cursor.getDayOfWeek() != targetDay) {
                        cursor = cursor.plusDays(1);
                    }
                    if (cursor.isBefore(pausedAt)) {
                        cursor = cursor.plusWeeks(1);
                    }

                    while (cursor.isBefore(now)) {
                        boolean olderThan8Hours = cursor.isBefore(now.minusHours(8));
                        LocalDateTime nextSlot = findNextSlotAfter(cursor, entries);
                        boolean nextSlotHasPassed = nextSlot != null && nextSlot.isBefore(now);

                        if (olderThan8Hours || nextSlotHasPassed) {
                            missed.add(dayTime + "|ADVANCE");
                        } else {
                            // within 8 hours, next slot hasn't passed — dispense immediately on resume
                            missed.add(dayTime + "|DISPENSE");
                        }

                        cursor = cursor.plusWeeks(1);
                    }
                });

        Map<String, Integer> dayOrder = Map.of(
                "monday", 0, "tuesday", 1, "wednesday", 2, "thursday", 3,
                "friday", 4, "saturday", 5, "sunday", 6
        );

        missed.sort((a, b) -> {
            String[] partsA = a.split("\\|");
            String[] partsB = b.split("\\|");
            int dayCompare = Integer.compare(
                    dayOrder.getOrDefault(partsA[0], 0),
                    dayOrder.getOrDefault(partsB[0], 0)
            );
            if (dayCompare != 0) return dayCompare;
            return LocalTime.parse(partsA[1]).compareTo(LocalTime.parse(partsB[1]));
        });

        return missed;
    }

    private LocalDateTime findNextSlotAfter(LocalDateTime after, List<ScheduleEntry> entries) {
        return entries.stream()
                .map(e -> e.getDay() + "|" + e.getTime())
                .distinct()
                .map(dayTime -> {
                    String[] parts = dayTime.split("\\|");
                    LocalTime time = LocalTime.parse(parts[1]);
                    DayOfWeek targetDay = DayOfWeek.valueOf(parts[0].toUpperCase());

                    LocalDateTime cursor = after.plusMinutes(1).toLocalDate().atTime(time);
                    while (cursor.getDayOfWeek() != targetDay) cursor = cursor.plusDays(1);
                    if (!cursor.isAfter(after)) cursor = cursor.plusWeeks(1);
                    return cursor;
                })
                .filter(dt -> dt.isAfter(after))
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * clears all slot assignments when the carousel is emptied
     * @param user the user whose dispenser has been emptied
     */
    @Transactional
    public void clearSlots(User user) {
        List<Slot> slots = slotRepository.findByUserOrderBySlotNumber(user);
        slots.forEach(s -> {
            s.setAssignedDay(null);
            s.setAssignedTime(null);
        });
        slotRepository.saveAll(slots);

        user.setLastFillDate(null);
        user.setScheduleChanged(false);

        userRepository.saveAndFlush(user);
    }

    /**
     * flags that the carousel should be emptied on the next firmware poll
     * @param user the user whose dispenser is to be emptied
     */
    @Transactional
    public void requestEmpty(User user) {
        user.setPendingEmpty(true);
        userRepository.save(user);
    }
}