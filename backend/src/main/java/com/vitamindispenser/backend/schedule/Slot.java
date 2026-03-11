package com.vitamindispenser.backend.schedule;


import com.vitamindispenser.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "slots")
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer slotNumber;

    @Column(nullable = false)
    private Boolean reserved; //true only for slot 15

    private String assignedDay;
    private String assignedTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
