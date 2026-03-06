package com.sebastiandorata.musicdashboard.Service;

import com.sebastiandorata.musicdashboard.DataAccess.UserRepository;
import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    // Login with email OR username
    public Optional<User> login(String usernameOrEmail, String password) {
        // Try to find by email first
        Optional<User> userOptional = userRepository.findByEmail(usernameOrEmail);

        // If not found by email, try username
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(usernameOrEmail);
        }

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Check if password matches
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }
}