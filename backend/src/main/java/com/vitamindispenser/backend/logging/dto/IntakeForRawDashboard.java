package com.vitamindispenser.backend.logging.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntakeForRawDashboard {
    private String vitaminType;
    private String day;
    private String time;
    private Integer numberOfPills;
    private Boolean taken;
    public IntakeForRawDashboard() {}
    public IntakeForRawDashboard(String vitaminType, String day, String time, Integer numberOfPills, Boolean taken) {
        this.vitaminType = vitaminType;
        this.day = day;
        this.time = time;
        this.numberOfPills = numberOfPills;
        this.taken = taken;
    }
}
