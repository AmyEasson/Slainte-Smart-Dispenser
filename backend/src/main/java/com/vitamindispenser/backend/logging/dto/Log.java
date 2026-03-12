package com.vitamindispenser.backend.logging.dto;


import lombok.Getter;
import lombok.Setter;

/**
 * Response body for intake log history sent to mobile app
 */
@Getter
@Setter
public class Log {

    private String vitaminType;
    private String day;
    private String time;
    private Integer numberOfPills;
    private Integer id;
    private Boolean taken;

    public Log() {}

    public Log(Integer numberOfPills, String vitaminType, String day, String time, Integer id) {
        this.numberOfPills = numberOfPills;
        this.vitaminType = vitaminType;
        this.day = day;
        this.time = time;
        this.id = id;
        this.taken = false;
    }
}
