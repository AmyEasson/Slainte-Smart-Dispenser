package com.vitamindispenser.backend.dto.logging;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "intake_attempts")
@Getter
@Setter
public class IntakeAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer intakeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntakeStatus status;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Column
    private Instant reportedAt;
}
