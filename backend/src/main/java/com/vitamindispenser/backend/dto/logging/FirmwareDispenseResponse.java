package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirmwareDispenseResponse {
    /**
     * References the DispenseSchedule.id that this dispense is for.
     * Firmware will report this ID back when confirming intake status
     */
    private Integer intakeId;
    private String vitaminType;
    private Integer numberOfPills;
}
