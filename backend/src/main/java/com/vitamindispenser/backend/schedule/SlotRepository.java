package com.vitamindispenser.backend.schedule;

import com.vitamindispenser.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Integer> {
    List<Slot> findByUserOrderBySlotNumber(User user);
    void deleteByUser(User user);
}
