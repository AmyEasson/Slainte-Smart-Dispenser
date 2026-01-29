package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class IntakeLogResponse {
    private Integer intakeId;
    private IntakeStatus status;
    private Instant reportedAt;
}
