package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body from firmware reporting vitamin dispensing status
 */
@Getter
@Setter
public class VitaminStatusRequest {

    private Boolean vitaminTaken;  // true = successfully dispensed, false = failed

    // Additional data we might want later
    private String vitaminType;
    private String timestamp;
}
