package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirmwareDispenseResponse {
    private Integer intakeId;
    private String vitaminType;
    private Integer numberOfPills;
}
