package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.User;
import com.facilitymaintenance.backend.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for database operations
 * involving application users.
 *
 * Spring Data JPA automatically implements these
 * methods based on their names.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Checks whether an email address already exists.
     *
     * Useful for preventing duplicate user accounts.
     *
     * @param email email address to check
     * @return true if the email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves a user by their email address.
     *
     * Optional is used because a matching user
     * might not exist in the database.
     *
     * @param email email address to search for
     * @return matching user, if one exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrieves all users belonging to a specific role.
     *
     * For automatic technician assignment, we call:
     *
     * findByRole(UserRole.TECHNICIAN)
     *
     * This returns all technicians so the service
     * can compare their active workloads.
     *
     * @param role application role to search for
     * @return users belonging to the specified role
     */
    List<User> findByRole(UserRole role);
}