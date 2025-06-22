package com.banking.expenseplanner.repository;

import com.banking.expenseplanner.model.Budget;
import com.banking.expenseplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser(User user);
    List<Budget> findByUserAndYear(User user, Integer year);
    List<Budget> findByUserAndMonth(User user, Integer month);
    List<Budget> findByUserAndMonthAndYear(User user, Integer month, Integer year);
    Optional<Budget> findByUserAndCategoryAndMonthAndYear(User user, String category, int month, int year);

}