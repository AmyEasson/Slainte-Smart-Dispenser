package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * Response body for intake log history sent to mobile app
 */
@Getter
@Setter
public class DispenseEvent {

    private String vitaminType;
    private String day;
    private String time;
    private Boolean taken;
    private Integer numberOfPills;
    private Integer id;

    // Constructors
    public DispenseEvent() {}

    public DispenseEvent(Integer numberOfPills, String vitaminType, String day, String time, Boolean taken, Integer id) {

        this.numberOfPills = numberOfPills;
        this.vitaminType = vitaminType;
        this.day = day;
        this.time = time;
        this.taken = taken;
        this.id = id;
    }
}