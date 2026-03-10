package com.vitamindispenser.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "schedule_entries")
public class ScheduleEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String vitaminType;
    private Integer numberOfPills;
    @Column(name = "day_of_week")
    private String day;
    @Column(name = "dispense_time")
    private String time;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;
}
