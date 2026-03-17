package com.vitamindispenser.backend.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity for user management. User should be able to log in
 * using a username and password.
 */

// TODO: pause/resume state and slot tracking should move to Device entity
// when one-to-one user-device mapping is enforced post-demo

@Getter
@Setter
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; //aim to store hashed, never plaintext

    private String role = "ROLE_USER";

    private LocalDate lastFillDate;
    @Column(nullable = false)
    private boolean scheduleChanged = false;

    @Column(nullable = false)
    private int fillCycleOffset = 0;

    @Column(nullable = false)
    private boolean paused = false;

    @Column(nullable = true)
    private LocalDateTime pausedAt;

    @Column(nullable = false)
    private int slotsToAdvance = 0;

    // json array of day/time strings to log as missed on an ADVANCE command
    @Column(columnDefinition = "TEXT")
    private String missedSlotQueue;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonExpired() {return true;}
    @Override
    public boolean isAccountNonLocked() {return true;}
    @Override
    public boolean isCredentialsNonExpired() {return true;}
    @Override
    public boolean isEnabled() {return true;}
}
