package com.vitamindispenser.backend.firmware.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request body from firmware reporting vitamin dispensing status
 */
@Getter
@Setter
public class IntakeReport {
    private List<Integer> intakeIds;      // the intake to which the status relates to
    private Boolean dispenseEventStatus;  // true = successfully dispensed, false = failed
}
