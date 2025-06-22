package com.banking.expenseplanner.repository;

import com.banking.expenseplanner.model.Reminder;
import com.banking.expenseplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUser(User user);
    List<Reminder> findByUserAndDueDate(User user, LocalDate dueDate);
    List<Reminder> findByUserAndDueDateLessThanEqual(User user, LocalDate date);
    List<Reminder> findByUserAndDueDateBetween(User user, LocalDate start, LocalDate end);
    List<Reminder> findByUserAndDueDateBetweenOrderByDueDateAsc(User user, LocalDate start, LocalDate end);
}