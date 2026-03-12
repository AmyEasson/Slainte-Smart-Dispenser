package com.vitamindispenser.backend.schedule.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a single vitamin type with its dispensing schedule
 */
@JsonPropertyOrder({ "vitaminType", "numberOfPills", "schedule" })
@Getter
@Setter
public class VitaminSchedule {

    private String vitaminType;
    private Integer numberOfPills;     // How many pills to dispense each time
    private List<DaySchedule> schedule; // Schedule organised by day

}