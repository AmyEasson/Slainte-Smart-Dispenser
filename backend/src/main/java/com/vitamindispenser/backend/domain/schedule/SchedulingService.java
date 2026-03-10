package com.vitamindispenser.backend.domain.schedule;


import com.vitamindispenser.backend.dto.schedule.DaySchedule;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import com.vitamindispenser.backend.dto.schedule.VitaminSchedule;
import com.vitamindispenser.backend.model.ScheduleEntry;
import com.vitamindispenser.backend.model.Slot;
import com.vitamindispenser.backend.model.User;
import com.vitamindispenser.backend.repository.ScheduleEntryRepository;
import com.vitamindispenser.backend.repository.SlotRepository;
import com.vitamindispenser.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulingService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Autowired
    public SchedulingService(ScheduleEntryRepository scheduleEntryRepository, SlotRepository slotRepository, UserRepository userRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    /**
     * Saves a schedule request by flattening the nested structure into individual DispenseEvents
     * @param request The schedule request from the mobile app
     */

    @Transactional
    public void saveSchedule(ScheduleRequest request, User user) {
        // delete existing schedule for this user first
        scheduleEntryRepository.deleteByUser(user);
        slotRepository.deleteByUser(user);

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
        if (user.getLastFillDate() != null) {
            user.setScheduleChanged(true);
            userRepository.save(user);
        }
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

    /**
     * Helper that creates 15 slots for a user the first time they save a schedule.
     * Slot 15 is reserved as it is the empty slot that faces the dispensing hole.
     * @param user the User that corresponds to the schedule sent
     */
    private void initialiseSlots(User user){
        List<Slot> slots = new ArrayList<>();
        for (int i =1; i <= 15; i++){
            Slot slot = new Slot();
            slot.setSlotNumber(i);
            slot.setReserved(i == 15);
            slot.setUser(user);
            slots.add(slot);
        }
        slotRepository.saveAll(slots);
    }


    /**
     * After saving schedule entries, group them by unique (day, time) pairs and
     * repeatedly assign the weekly schedule to slots 1-14 until all slots are filled
     * @param entries schedule entries inputted by the user
     * @param user the user who sent the schedule
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
            throw new IllegalArgumentException("NO_SCHEDULE");
        }

        if (uniqueTimes.size() > 14) {
            throw new IllegalArgumentException("SCHEDULE_TOO_LARGE");
        }

        slots.forEach(s -> {
            s.setAssignedDay(null);
            s.setAssignedTime(null);
        });

        for (int i = 0; i < 14; i++) {
            String[] dayTime = uniqueTimes.get(i % uniqueTimes.size());
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

            // look up vitamins by day+time
            List<Map<String, Object>> vitamins = (slot.getAssignedDay() == null)
                    ? List.of()
                    : scheduleEntryRepository
                    .findByUserAndDayAndTime(user, slot.getAssignedDay(), slot.getAssignedTime())
                    .stream()
                    .map(e -> {
                        Map<String, Object> v = new LinkedHashMap<>();
                        v.put("vitaminType", e.getVitaminType());
                        v.put("numberOfPills", e.getNumberOfPills());
                        return v;
                    })
                    .toList();

            slotMap.put("vitamins", vitamins);
            return slotMap;
        }).toList();

        return Map.of("slots", slotList);
    }

    public Map<String, Object> getRefillInfo(User user) {
        List<Slot> slots = slotRepository.findByUserOrderBySlotNumber(user);

        long totalSlotsNeeded = slots.stream()
                .filter(s -> !s.getReserved() && s.getAssignedDay() != null)
                .count();

        Map<String, Object> result = new LinkedHashMap<>();

        if (totalSlotsNeeded == 0) {
            result.put("lastFillDate", null);
            result.put("refillDate", null);
            result.put("daysUntilRefill", null);
            result.put("warning", "NO_SCHEDULE");
            return result;
        }

        if (totalSlotsNeeded > 14) {
            result.put("lastFillDate", null);
            result.put("refillDate", null);
            result.put("daysUntilRefill", null);
            result.put("warning", "SCHEDULE_TOO_LARGE");
            return result;
        }

        long daysPerFill = (14 / totalSlotsNeeded) * 7;
        // Cap at 90 days
        daysPerFill = Math.min(daysPerFill, 90);

        LocalDate lastFillDate = user.getLastFillDate() != null
                ? user.getLastFillDate()
                : LocalDate.now();

        LocalDate refillDate = lastFillDate.plusDays(daysPerFill);
        long daysUntilRefill = ChronoUnit.DAYS.between(LocalDate.now(), refillDate);

        result.put("lastFillDate", lastFillDate.toString());
        result.put("refillDate", refillDate.toString());
        result.put("daysUntilRefill", daysUntilRefill);
        result.put("warning", user.isScheduleChanged() ? "SCHEDULE_CHANGED" : null);

        return result;
    }

    public Map<String, Object> confirmFill(User user) {
        user.setLastFillDate(LocalDate.now());
        user.setScheduleChanged(false);
        userRepository.save(user);

        List<Slot> slots = slotRepository.findByUserOrderBySlotNumber(user);
        long totalSlotsNeeded = slots.stream()
                .filter(s -> !s.getReserved() && s.getAssignedDay() != null)
                .count();

        long daysPerFill = totalSlotsNeeded > 0
                ? Math.min((14 / totalSlotsNeeded) * 7, 90)
                : 0;

        LocalDate refillDate = LocalDate.now().plusDays(daysPerFill);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lastFillDate", LocalDate.now().toString());
        result.put("refillDate", refillDate.toString());
        result.put("daysUntilRefill", daysPerFill);
        return result;
    }

}


