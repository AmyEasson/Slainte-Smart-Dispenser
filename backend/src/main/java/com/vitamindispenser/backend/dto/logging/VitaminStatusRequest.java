package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body from firmware reporting vitamin dispensing status
 */
@Getter
@Setter
public class VitaminStatusRequest {

    private Integer intakeId;      // the intake to which the status relates to
    private Boolean vitaminTaken;  // true = successfully dispensed, false = failed
}
