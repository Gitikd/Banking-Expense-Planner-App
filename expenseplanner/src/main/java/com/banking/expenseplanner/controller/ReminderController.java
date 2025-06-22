package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.model.Reminder;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.model.Expense;
import com.banking.expenseplanner.repository.ReminderRepository;
import com.banking.expenseplanner.repository.ExpenseRepository;
import com.banking.expenseplanner.repository.UserRepository;
import com.banking.expenseplanner.util.UserUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;


import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserUtil userUtil;

    @PostMapping("/add")
    public ResponseEntity<?> addReminder(@Valid @RequestBody Reminder reminder) {
        User user = userUtil.getLoggedInUser();
        reminder.setUser(user);
        reminderRepository.save(reminder);
        return ResponseEntity.ok("Reminder added successfully");
    }

    @GetMapping("/my")
    public ResponseEntity<?> getRemindersByUser() {
        User user = userUtil.getLoggedInUser();
        return ResponseEntity.ok(reminderRepository.findByUser(user));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingReminders() {
        User user = userUtil.getLoggedInUser();
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        // ✅ Get overdue + upcoming reminders
        List<Reminder> reminders = reminderRepository.findByUserAndDueDateLessThanEqual(user, nextWeek);
        return ResponseEntity.ok(reminders);
    }



    @PutMapping("/pay/{id}")
    public ResponseEntity<?> payReminder(@PathVariable Long id) {
        Optional<Reminder> reminderOpt = reminderRepository.findById(id);
        if (reminderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Reminder not found");
        }

        Reminder reminder = reminderOpt.get();
        User user = userUtil.getLoggedInUser();

        // Check balance
        if (user.getBalance() < reminder.getAmount()) {
            return ResponseEntity.badRequest().body("Insufficient balance to pay bill");
        }

        // Deduct balance
        user.setBalance(user.getBalance() - reminder.getAmount());
        userRepository.save(user);

        // Add expense
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(reminder.getAmount());
        expense.setCategory(reminder.getTitle()); // 🟢 title becomes category
        expense.setDate(LocalDate.now());
        expense.setDescription(reminder.getNote() != null ? reminder.getNote() : "Paid bill"); // 🟢 note becomes description
        expense.setType("EXPENSE");
        expenseRepository.save(expense);

        // Handle recurring
        if (reminder.isRecurring()) {
            reminder.setDueDate(reminder.getDueDate().plusMonths(1));
            reminderRepository.save(reminder);
            return ResponseEntity.ok("Reminder paid and rescheduled for next month");
        } else {
            reminderRepository.delete(reminder);
            return ResponseEntity.ok("Reminder paid and removed");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReminder(@PathVariable Long id, @RequestBody Reminder updated) {
        Optional<Reminder> reminderOpt = reminderRepository.findById(id);
        if (reminderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Reminder not found");
        }

        Reminder reminder = reminderOpt.get();
        User user = userUtil.getLoggedInUser();
        if (!reminder.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        reminder.setTitle(updated.getTitle());
        reminder.setNote(updated.getNote());
        reminder.setDueDate(updated.getDueDate());
        reminder.setRecurring(updated.isRecurring());
        reminder.setAmount(updated.getAmount());

        reminderRepository.save(reminder);
        return ResponseEntity.ok("Reminder updated successfully");
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long id) {
        Optional<Reminder> opt = reminderRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body("Reminder not found");

        Reminder reminder = opt.get();
        User user = userUtil.getLoggedInUser();

        if (!reminder.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        reminderRepository.delete(reminder);
        return ResponseEntity.ok("Reminder deleted successfully!");
    }


}