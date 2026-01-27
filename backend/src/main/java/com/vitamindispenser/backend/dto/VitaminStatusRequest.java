package com.vitamindispenser.backend.dto;

/**
 * Request body from firmware reporting vitamin dispensing status
 */
public class VitaminStatusRequest {

    private Boolean vitaminTaken;  // true = successfully dispensed, false = failed

    // Additional data we might want later
    private String vitaminType;
    private String timestamp;

    // Getters and Setters
    public Boolean getVitaminTaken() {
        return vitaminTaken;
    }

    public void setVitaminTaken(Boolean vitaminTaken) {
        this.vitaminTaken = vitaminTaken;
    }

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
}
