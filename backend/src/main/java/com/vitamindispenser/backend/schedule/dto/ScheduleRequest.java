package com.vitamindispenser.backend.schedule.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request body for creating/updating vitamin schedule from mobile app
 *
 * Example JSON:
 * {
 *   "vitamins": [
 *     {
 *       "vitaminType": "Vitamin A",
 *       "numberOfPills": 2,
 *       "schedule": [
 *         {"day": "monday", "times": ["10:00", "12:00"]},
 *         {"day": "tuesday", "times": ["10:00", "12:00"]}
 *       ]
 *     }
 *   ]
 * }
 */
@Getter
@Setter
@JsonPropertyOrder({ "vitamins" })
public class ScheduleRequest {
    // List of all vitamins and their schedules
    private List<VitaminSchedule> vitamins;
}
