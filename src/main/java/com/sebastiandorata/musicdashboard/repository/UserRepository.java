package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides finders and existence checks by email and username, used
 * during login (find by email or username) and registration (check for
 * duplicates before creating a new account).</p>
 */
public interface UserRepository extends JpaRepository <User, Long>{

    Optional<User> findByEmail(String email);

    // Check if email already exists
    boolean existsByEmail(String email);

    // Find user by username
    Optional<User> findByUsername(String username);

    // Check if username already exists
    boolean existsByUsername(String username);
}
