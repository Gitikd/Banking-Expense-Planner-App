package com.banking.expenseplanner.repository;

import com.banking.expenseplanner.model.SavingsGoal;
import com.banking.expenseplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);
}