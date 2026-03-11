package com.vitamindispenser.backend.schedule.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the schedule for a specific day of the week
 */
@Getter
@Setter
@JsonPropertyOrder({ "day", "times" })
public class DaySchedule {

    private String day;
    private List<String> times;

}
