package com.vitamindispenser.backend.device;


import com.vitamindispenser.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// TODO: pause/resume state and slot tracking should move to Device entity
// when one-to-one user-device mapping is enforced post-demo

@Entity
@Getter
@Setter
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
}
