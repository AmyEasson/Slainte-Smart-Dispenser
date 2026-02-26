package com.vitamindispenser.backend.repository;


import com.vitamindispenser.backend.model.LoggingDatabase;
import com.vitamindispenser.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispenseEventLogRepository
        extends JpaRepository<LoggingDatabase, Long> {
    List<LoggingDatabase> findByUser(User user);
}
