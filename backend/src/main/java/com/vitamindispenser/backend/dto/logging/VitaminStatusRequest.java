package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request body from firmware reporting vitamin dispensing status
 */
@Getter
@Setter
public class VitaminStatusRequest {

    private List<Integer> intakeIds;      // the intake to which the status relates to
    private Boolean dispenseEventStatus;  // true = successfully dispensed, false = failed
}
