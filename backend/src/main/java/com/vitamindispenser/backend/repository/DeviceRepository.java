package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.model.Device;
import com.vitamindispenser.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);
    Optional<Device> findByOwner(User owner);
}
