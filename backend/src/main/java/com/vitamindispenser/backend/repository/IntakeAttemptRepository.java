package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.dto.logging.IntakeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntakeAttemptRepository extends JpaRepository<IntakeAttempt, Long> {

    // Find the most recent attempt for a given intakeId
    Optional<IntakeAttempt> findFirstByIntakeIdOrderByScheduledAtDesc(Integer intakeId);

    // Check if there's already an attempt for this intakeId today
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM IntakeAttempt a " +
            "WHERE a.intakeId = :intakeId " +
            "AND a.scheduledAt >= :startOfDay " +
            "AND a.scheduledAt < :endOfDay")
    boolean existsTodayByIntakeId(Integer intakeId, Instant startOfDay, Instant endOfDay);

    // Get all attempts ordered newest first
    List<IntakeAttempt> findAllByOrderByScheduledAtDesc();
}
