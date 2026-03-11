package com.vitamindispenser.backend.config;

import com.vitamindispenser.backend.device.Device;
import com.vitamindispenser.backend.device.DeviceRepository;
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
