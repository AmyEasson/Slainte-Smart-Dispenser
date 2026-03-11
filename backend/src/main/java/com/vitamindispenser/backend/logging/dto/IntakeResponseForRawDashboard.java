package com.vitamindispenser.backend.logging.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IntakeResponseForRawDashboard {

    private List<IntakeForRawDashboard> data;
    private String message;

    public IntakeResponseForRawDashboard() {
    }

    public IntakeResponseForRawDashboard(List<IntakeForRawDashboard> data, String message) {
        this.data = data;
        this.message = message;
    }

}
