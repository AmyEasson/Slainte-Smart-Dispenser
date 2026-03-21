package com.vitamindispenser.backend.auth;

import com.vitamindispenser.backend.device.Device;
import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.logging.DispenseEventLogRepository;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import com.vitamindispenser.backend.user.User;
import com.vitamindispenser.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final DeviceRepository deviceRepository;
    private final DispenseEventLogRepository dispenseEventLogRepository;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          DeviceRepository deviceRepository, DispenseEventLogRepository dispenseEventLogRepository,
                          ScheduleEntryRepository scheduleEntryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deviceRepository = deviceRepository;
        this.dispenseEventLogRepository = dispenseEventLogRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteAccount(User user) {
        dispenseEventLogRepository.findByUser(user)
                .forEach(dispenseEventLogRepository::delete);
        scheduleEntryRepository.deleteByUser(user);
        deviceRepository.findByOwner(user).ifPresent(device -> {
            device.setOwner(null);
            deviceRepository.save(device);
        });
        userRepository.delete(user);
    }
}
