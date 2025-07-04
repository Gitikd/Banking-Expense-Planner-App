package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.model.Expense;
import com.banking.expenseplanner.model.SavingsGoal;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.ExpenseRepository;
import com.banking.expenseplanner.repository.SavingsGoalRepository;
import com.banking.expenseplanner.repository.UserRepository;
import com.banking.expenseplanner.util.UserUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/savings")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private UserUtil userUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addGoal(@Valid @RequestBody SavingsGoal goal) {
        User user = userUtil.getLoggedInUser();
        goal.setUser(user);
        savingsGoalRepository.save(goal);
        return ResponseEntity.ok("Savings goal created successfully");
    }

    @GetMapping("/my")
    public ResponseEntity<?> getUserGoals() {
        User user = userUtil.getLoggedInUser();
        return ResponseEntity.ok(savingsGoalRepository.findByUser(user));
    }

    @PutMapping("/update/{goalId}")
    public ResponseEntity<?> updateGoalProgress(@PathVariable Long goalId, @RequestParam Double amountToAdd) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Goal not found");
        }

        SavingsGoal goal = goalOpt.get();
        User user = userUtil.getLoggedInUser();

        if (!goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied: This goal does not belong to you.");
        }

        // ✅ Check if already completed
        if ("COMPLETED".equalsIgnoreCase(goal.getStatus())) {
            return ResponseEntity.badRequest().body("This goal is already completed. Cannot add more.");
        }

        double remainingToTarget = goal.getTargetAmount() - goal.getSavedAmount();

        if (remainingToTarget <= 0) {
            return ResponseEntity.badRequest().body("Goal already fully saved.");
        }

        double amountToUse = Math.min(amountToAdd, remainingToTarget);

        // ✅ Check balance before deducting
        if (user.getBalance() < amountToUse) {
            return ResponseEntity.badRequest().body("Insufficient balance to add this amount.");
        }

        // ✅ Deduct only what is used
        user.setBalance(user.getBalance() - amountToUse);
        goal.setSavedAmount(goal.getSavedAmount() + amountToUse);

        // ✅ Mark as completed if goal met
        if (goal.getSavedAmount() >= goal.getTargetAmount()) {
            goal.setStatus("COMPLETED");
        }

        userRepository.save(user);
        savingsGoalRepository.save(goal);

        return ResponseEntity.ok("Progress updated successfully!");
    }


    //Withdraw Money from Saved Amount
    @PutMapping("/withdraw/{goalId}")
    public ResponseEntity<?> withdrawFromGoal(@PathVariable Long goalId, @RequestParam Double amount) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Goal not found");
        }

        SavingsGoal goal = goalOpt.get();
        User user = userUtil.getLoggedInUser();

        if (!goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        if (amount <= 0 || amount > goal.getSavedAmount()) {
            return ResponseEntity.badRequest().body("Invalid withdraw amount");
        }

        goal.setSavedAmount(goal.getSavedAmount() - amount);
        if (goal.getStatus().equalsIgnoreCase("COMPLETED") && goal.getSavedAmount() < goal.getTargetAmount()) {
            goal.setStatus("IN_PROGRESS");
        }

        user.setBalance(user.getBalance() + amount);

        savingsGoalRepository.save(goal);
        userRepository.save(user);

        return ResponseEntity.ok("Amount withdrawn successfully");
    }


//    🛍 1. Buy Goal → Add to transactions and mark as PURCHASED
    @PutMapping("/buy/{goalId}")
    public ResponseEntity<?> buyGoal(@PathVariable Long goalId) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Goal not found");
        }

        SavingsGoal goal = goalOpt.get();
        User user = userUtil.getLoggedInUser();

        if (!goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied: This goal does not belong to you.");
        }

        if (!"COMPLETED".equalsIgnoreCase(goal.getStatus())) {
            return ResponseEntity.badRequest().body("Goal is not yet completed.");
        }

        // Record as an expense transaction
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(goal.getTargetAmount());
        expense.setCategory(goal.getGoalName());
        expense.setDescription(goal.getDescription());
        expense.setDate(LocalDate.now());
        expense.setType("EXPENSE");

        expenseRepository.save(expense);


        // Mark goal as purchased
        goal.setStatus("PURCHASED");
        savingsGoalRepository.save(goal);

        return ResponseEntity.ok("Goal marked as purchased and added to transactions.");
    }

//    💸 2. Withdraw Goal → Refund saved amount and delete goal
    @DeleteMapping("/withdraw/{goalId}")
    public ResponseEntity<?> withdrawGoal(@PathVariable Long goalId) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(goalId);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Goal not found");
        }

        SavingsGoal goal = goalOpt.get();
        User user = userUtil.getLoggedInUser();

        if (!goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied: This goal does not belong to you.");
        }

        if ("PURCHASED".equalsIgnoreCase(goal.getStatus())) {
            return ResponseEntity.badRequest().body("This goal is already purchased and cannot be withdrawn.");
        }

//        if ("COMPLETED".equalsIgnoreCase(goal.getStatus())) {
//            return ResponseEntity.badRequest().body("Completed goals must be bought, not withdrawn.");
//        }

        // Add saved amount back to balance
        user.setBalance(user.getBalance() + goal.getSavedAmount());
        userRepository.save(user);

        // Delete the goal
        savingsGoalRepository.delete(goal);

        return ResponseEntity.ok("Goal withdrawn. Saved amount returned to balance.");
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findById(id);
        if (goalOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Goal not found");
        }

        SavingsGoal goal = goalOpt.get();
        User user = userUtil.getLoggedInUser();

        if (!goal.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        // ❌ Prevent deleting completed goals
        if ("COMPLETED".equalsIgnoreCase(goal.getStatus())) {
            return ResponseEntity.badRequest().body("Cannot delete a completed goal.");
        }

        // ✅ Add savedAmount back to balance
        user.setBalance(user.getBalance() + goal.getSavedAmount());
        userRepository.save(user);

        savingsGoalRepository.delete(goal);
        return ResponseEntity.ok("Goal deleted and amount returned to balance.");
    }



}