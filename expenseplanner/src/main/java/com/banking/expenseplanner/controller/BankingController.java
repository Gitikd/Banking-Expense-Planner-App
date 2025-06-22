package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.model.Expense;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.ExpenseRepository;
import com.banking.expenseplanner.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banking")
public class BankingController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserUtil userUtil;

    @GetMapping("/summary")
    public ResponseEntity<?> getAccountSummary() {
        User user = userUtil.getLoggedInUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        double totalIncome = expenses.stream()
                .filter(e -> "INCOME".equalsIgnoreCase(e.getType()))
                .mapToDouble(Expense::getAmount)
                .sum();

        double totalExpense = expenses.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getType()))
                .mapToDouble(Expense::getAmount)
                .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("user", user.getName());
        response.put("totalBalance", user.getBalance());
        response.put("totalIncome", totalIncome);
        response.put("totalExpense", totalExpense);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactionHistory() {
        User user = userUtil.getLoggedInUser();
        List<Expense> expenses = expenseRepository.findByUser(user);
        return ResponseEntity.ok(expenses);
    }
}
