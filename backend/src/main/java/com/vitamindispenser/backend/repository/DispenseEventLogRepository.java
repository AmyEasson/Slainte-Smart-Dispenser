package com.vitamindispenser.backend.repository;


import com.vitamindispenser.backend.DispenseEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispenseEventLogRepository
        extends JpaRepository<DispenseEventLog, Long> {
}
