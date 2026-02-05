package com.vitamindispenser.backend.dto.logging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

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
