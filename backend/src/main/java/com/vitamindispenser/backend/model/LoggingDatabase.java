package com.vitamindispenser.backend.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/*
This is a Java representation of a database table row
Each instance of DispenseEventLog = one row in a SQL table
JPA/Hibernate use this class to generate SQL and save/read data.

Warning: Deploying this app somewhere might mean that we need to migrate to Postgres/MySQL.
 */

@Entity
@Table(name = "dispense_event_log")
@Getter
@Setter
public class LoggingDatabase {
    /*
    logId is the primary key
    Database generates it automatically
    You never set it manually
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Integer intakeId;

    @Column(nullable = false)
    private String vitaminType;

    @Column(nullable = false)
    private Integer numberOfPills;

    @Column(name = "day_name", nullable = false)
    private String day;

    @Column(name = "time_value", nullable = false)
    private String time;

    @Column(nullable = false)
    private Boolean taken;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

