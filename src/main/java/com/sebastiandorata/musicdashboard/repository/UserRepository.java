package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long>{

    Optional<User> findByEmail(String email);

    // Check if email already exists
    boolean existsByEmail(String email);

    // Find user by username
    Optional<User> findByUsername(String username);

    // Check if username already exists
    boolean existsByUsername(String username);
}
