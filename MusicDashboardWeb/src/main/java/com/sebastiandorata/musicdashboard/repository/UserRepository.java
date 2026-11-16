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

    /**
     * Finds a user by exact email address.
     *
     * @param email the email address to match
     * @return the matching user, or an empty {@code Optional} if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an account already uses the given email address.
     *
     * @param email the email address to check
     * @return {@code true} if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by exact username.
     *
     * @param username the username to match
     * @return the matching user, or an empty {@code Optional} if none exists
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether an account already uses the given username.
     *
     * @param username the username to check
     * @return {@code true} if a user with this username exists
     */
    boolean existsByUsername(String username);
}
