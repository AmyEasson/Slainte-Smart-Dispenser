package com.vitamindispenser.backend.logging;


import com.vitamindispenser.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispenseEventLogRepository
        extends JpaRepository<LoggingDatabase, Long> {
    List<LoggingDatabase> findByUser(User user);
}
