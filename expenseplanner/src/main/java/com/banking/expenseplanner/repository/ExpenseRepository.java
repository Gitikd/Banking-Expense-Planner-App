package com.banking.expenseplanner.repository;

import com.banking.expenseplanner.model.Expense;
import com.banking.expenseplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Get all expenses for a specific user
    List<Expense> findByUser(User user);
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
    	       "WHERE e.user.id = :userId AND e.category = :category " +
    	       "AND FUNCTION('MONTH', e.date) = :month AND FUNCTION('YEAR', e.date) = :year")
    	double sumAmountByUserAndCategoryAndMonthAndYear(Long userId, String category, int month, int year);

    List<Expense> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate start, LocalDate end);

}
