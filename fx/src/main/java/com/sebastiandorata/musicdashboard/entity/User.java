package com.sebastiandorata.musicdashboard.entity;

import com.sebastiandorata.musicdashboard.controller.Authentication.SecurityConfig;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing an application user.
 *
 * <p>Stores login credentials (email, BCrypt-hashed password, unique
 * username), the account creation timestamp, and the last login
 * timestamp. Passwords are never stored in plain text; hashing is
 * delegated to {@link SecurityConfig}.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;


    public User() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}