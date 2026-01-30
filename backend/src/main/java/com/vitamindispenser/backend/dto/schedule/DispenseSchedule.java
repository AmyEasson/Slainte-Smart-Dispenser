package com.vitamindispenser.backend.dto.schedule;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class DispenseSchedule {
    /**
     * Unique identifier for this schedule entry.
     * This id is used throughout the system as the 'intakeId'
     * to track dispense attempts and logs
     */
    private Integer id;
    private String vitaminType;
    private Integer numberOfPills;
    private DayOfWeek day;
    private LocalTime time;
}
