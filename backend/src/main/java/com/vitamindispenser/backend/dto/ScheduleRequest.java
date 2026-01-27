package com.vitamindispenser.backend.dto;

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

public class ScheduleRequest {
    // List of all vitamins and their schedules
    private List<VitaminSchedule> vitamins;

    // Getters and setters
    public List<VitaminSchedule> getVitamins() {
        return vitamins;
    }

    public void setVitamins(List<VitaminSchedule> vitamins) {
        this.vitamins = vitamins;
    }
}
