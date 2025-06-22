package com.banking.expenseplanner.repository;

import com.banking.expenseplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Custom method to find user by email
    Optional<User> findByEmail(String email);
}
