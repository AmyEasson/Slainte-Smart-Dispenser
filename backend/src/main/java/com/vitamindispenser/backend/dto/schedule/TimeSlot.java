package com.vitamindispenser.backend.dto.schedule;

import lombok.Getter;
import lombok.Setter;

/*
 each time = one dispense event with an ID.
 */
@Getter
@Setter
public class TimeSlot {
    private Integer id;
    private String time;
}