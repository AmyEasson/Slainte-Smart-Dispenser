package com.vitamindispenser.backend.dto.schedule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the schedule for a specific day of the week
 */
@Getter
@Setter
public class DaySchedule {

    private String day;
    private List<TimeSlot> times;

}
