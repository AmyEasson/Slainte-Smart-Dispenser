package com.vitamindispenser.backend.dto;

/**
 * Response body for intake log history sent to mobile app
 */
public class IntakeLogResponse {

    private String vitaminType;
    private String timestamp;
    private Boolean taken;

    // Constructors
    public IntakeLogResponse() {}

    public IntakeLogResponse(String vitaminType, String timestamp, Boolean taken) {
        this.vitaminType = vitaminType;
        this.timestamp = timestamp;
        this.taken = taken;
    }

    // Getters and Setters
    public String getVitaminType() {
        return vitaminType;
    }

    public void setVitaminType(String vitaminType) {
        this.vitaminType = vitaminType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getTaken() {
        return taken;
    }

    public void setTaken(Boolean taken) {
        this.taken = taken;
    }
}