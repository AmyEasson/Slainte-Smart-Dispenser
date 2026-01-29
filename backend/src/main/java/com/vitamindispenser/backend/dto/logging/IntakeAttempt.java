package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class IntakeAttempt {
    private Integer intakeId;
    private IntakeStatus status;
    private Instant scheduledAt;
    private Instant reportedAt;
}
