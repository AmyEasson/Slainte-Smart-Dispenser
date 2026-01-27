package com.vitamindispenser.backend.dto;

import java.util.List;

/**
 * Represents the schedule for a specific day of the week
 */
public class DaySchedule {

    private String day;
    private List<String> times;

    // Getters and Setters
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }
}
