package com.vitamindispenser.backend.firmware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Result of a poll: command for the firmware and the intake/slot ids when command is DISPENSE. */
@Getter
@AllArgsConstructor
public class PollCommandResult {
    private final String command;
    private final List<Integer> intakeIds;
    private final Integer slotNumber;
}
