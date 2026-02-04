package com.vitamindispenser.backend.repository;


import com.vitamindispenser.backend.dto.logging.LoggingDatabase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispenseEventLogRepository
        extends JpaRepository<LoggingDatabase, Long> {
}
