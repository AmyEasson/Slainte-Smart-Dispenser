package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * Response body for intake log history sent to mobile app
 */
@Getter
@Setter
public class IntakeLogResponse {

    private String vitaminType;
    private String timestamp;
    private Boolean taken;

    // Constructors
    public IntakeLogResponse() {}

    public IntakeLogResponse(String vitaminType, String timestamp, Boolean taken) {
        this.vitaminType = vitaminType;
        this.timestamp = timestamp;
        this.taken = taken;
    }
}