package com.vitamindispenser.backend.exceptions;

public class DeviceAlreadyClaimedException extends RuntimeException {
    public DeviceAlreadyClaimedException(String deviceId) {
        super("Device " + deviceId + " is already claimed by another user");
    }
}
