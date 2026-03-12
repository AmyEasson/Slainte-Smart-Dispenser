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

        assignSlots(entries, user);
    }

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
            throw new IllegalArgumentException("NO_SCHEDULE");
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
}