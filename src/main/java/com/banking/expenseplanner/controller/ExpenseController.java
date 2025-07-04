package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.model.Budget;
import com.banking.expenseplanner.model.Expense;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.BudgetRepository;
import com.banking.expenseplanner.repository.ExpenseRepository;
import com.banking.expenseplanner.repository.UserRepository;
import com.banking.expenseplanner.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserUtil userUtil;

    // ✅ 1. Create Expense
    @PostMapping("/add")
    public ResponseEntity<?> addExpense(@Valid @RequestBody Expense expense) {
        User user = userUtil.getLoggedInUser();

        // Set user
        expense.setUser(user);

        // 🔐 Only check for EXPENSE type
        if ("EXPENSE".equalsIgnoreCase(expense.getType())) {
            int month = expense.getDate().getMonthValue();
            int year = expense.getDate().getYear();

            // 🔍 Get matching budget
            Optional<Budget> budgetOpt = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                user, expense.getCategory(), month, year);

            if (budgetOpt.isPresent()) {
                Budget budget = budgetOpt.get();

                // 🔁 Get total spent in that category this month
                double alreadySpent = expenseRepository
                    .sumAmountByUserAndCategoryAndMonthAndYear(user.getId(), expense.getCategory(), month, year);

                double remainingBudget = budget.getLimitAmount() - alreadySpent;

                if (expense.getAmount() > remainingBudget) {
                    return ResponseEntity.badRequest().body("❌ Cannot add: Budget limit exceeded for this category.");
                }
            }

            // 🛑 Check balance
            if (user.getBalance() < expense.getAmount()) {
                return ResponseEntity.badRequest().body("❌ Insufficient balance.");
            }

            user.setBalance(user.getBalance() - expense.getAmount());
        } else if ("INCOME".equalsIgnoreCase(expense.getType())) {
            user.setBalance(user.getBalance() + expense.getAmount());
        }

        // 💾 Save all
        expenseRepository.save(expense);
        userRepository.save(user);

        return ResponseEntity.ok("Transaction added successfully");
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterExpensesByDate(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        User user = userUtil.getLoggedInUser();
        List<Expense> expenses = expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, start, end);
        return ResponseEntity.ok(expenses);
    }


    // ✅ 2. Get all expenses by logged-in user
    @GetMapping("/my")
    public ResponseEntity<?> getExpensesByUser(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        User user = userUtil.getLoggedInUser();
        List<Expense> allExpenses = expenseRepository.findByUser(user);

        if (month != null || year != null) {
            return ResponseEntity.ok(
                allExpenses.stream().filter(exp -> {
                    LocalDate date = exp.getDate();
                    boolean matchMonth = (month == null || date.getMonthValue() == month);
                    boolean matchYear = (year == null || date.getYear() == year);
                    return matchMonth && matchYear;
                }).sorted(Comparator.comparing(Expense::getDate).reversed()).toList()
            );
        }

        return ResponseEntity.ok(allExpenses.stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .toList());
    }


    // ✅ 3. Update expense
    @PutMapping("/update/{expenseId}")
    public String updateExpense(@PathVariable Long expenseId, @Valid @RequestBody Expense updatedExpense) {
        User loggedUser = userUtil.getLoggedInUser();
        Optional<Expense> existingOpt = expenseRepository.findById(expenseId);

        if (existingOpt.isPresent()) {
            Expense expense = existingOpt.get();

            // Check if the expense belongs to the logged-in user
            if (!expense.getUser().getId().equals(loggedUser.getId())) {
                return "Access denied: You can only update your own expenses.";
            }

            // Optional: Adjust user balance before & after update (advanced)
            expense.setCategory(updatedExpense.getCategory());
            expense.setDescription(updatedExpense.getDescription());
            expense.setAmount(updatedExpense.getAmount());
            expense.setDate(updatedExpense.getDate());
            expense.setType(updatedExpense.getType());

            expenseRepository.save(expense);
            return "Expense updated successfully!";
        } else {
            return "Expense not found!";
        }
    }

    // ✅ 4. Delete expense
    @DeleteMapping("/delete/{expenseId}")
    public String deleteExpense(@PathVariable Long expenseId) {
        User loggedUser = userUtil.getLoggedInUser();
        Optional<Expense> expenseOpt = expenseRepository.findById(expenseId);

        if (expenseOpt.isPresent()) {
            Expense expense = expenseOpt.get();

            // Check if the expense belongs to the logged-in user
            if (!expense.getUser().getId().equals(loggedUser.getId())) {
                return "Access denied: You can only delete your own expenses.";
            }

            // Reverse the balance effect
            if ("EXPENSE".equalsIgnoreCase(expense.getType())) {
                loggedUser.setBalance(loggedUser.getBalance() + expense.getAmount());
            } else if ("INCOME".equalsIgnoreCase(expense.getType())) {
                loggedUser.setBalance(loggedUser.getBalance() - expense.getAmount());
            }
            userRepository.save(loggedUser);

            expenseRepository.deleteById(expenseId);
            return "Expense deleted successfully!";
        } else {
            return "Expense not found!";
        }
    }
}
