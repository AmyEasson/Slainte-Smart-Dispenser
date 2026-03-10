package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.model.Slot;
import com.vitamindispenser.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Integer> {
    List<Slot> findByUserOrderBySlotNumber(User user);
    void deleteByUser(User user);
}
