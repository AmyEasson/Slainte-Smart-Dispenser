package com.vitamindispenser.backend.repository;

import com.vitamindispenser.backend.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public void run(String... args) {
        if (deviceRepository.findByDeviceId("DISPENSER_001") .isEmpty()){
            Device device = new Device();
            device.setDeviceId("DISPENSER_001");
            deviceRepository.save(device);
        }
    }
}
