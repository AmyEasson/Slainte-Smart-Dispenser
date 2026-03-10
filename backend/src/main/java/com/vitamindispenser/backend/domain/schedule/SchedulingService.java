package com.vitamindispenser.backend.domain.schedule;


import com.vitamindispenser.backend.dto.schedule.DaySchedule;
import com.vitamindispenser.backend.dto.schedule.ScheduleRequest;
import com.vitamindispenser.backend.dto.schedule.VitaminSchedule;
import com.vitamindispenser.backend.model.ScheduleEntry;
import com.vitamindispenser.backend.model.User;
import com.vitamindispenser.backend.repository.ScheduleEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchedulingService {

    private final ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    public SchedulingService(ScheduleEntryRepository scheduleEntryRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    /**
     * Saves a schedule request by flattening the nested structure into individual DispenseEvents
     * @param request The schedule request from the mobile app
     */

    @Transactional
    public void saveSchedule(ScheduleRequest request, User user) {
        // delete existing schedule for this user first
        scheduleEntryRepository.deleteByUser(user);

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
        scheduleEntryRepository.saveAll(entries);
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

}


