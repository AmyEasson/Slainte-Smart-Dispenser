package com.vitamindispenser.backend.dto;

import java.util.List;

/**
 * Represents a single vitamin type with its dispensing schedule
 */
public class VitaminSchedule {

    private String vitaminType;
    private Integer numberOfPills;     // How many pills to dispense each time
    private List<DaySchedule> schedule; // Schedule organised by day

    // Getters and Setters
    public String getVitaminType() {
        return vitaminType;
    }

    public void setVitaminType(String vitaminType) {
        this.vitaminType = vitaminType;
    }

    public Integer getNumberOfPills() {
        return numberOfPills;
    }

    public void setNumberOfPills(Integer numberOfPills) {
        this.numberOfPills = numberOfPills;
    }

    public List<DaySchedule> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<DaySchedule> schedule) {
        this.schedule = schedule;
    }
}