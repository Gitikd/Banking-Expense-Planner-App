package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.model.Budget;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.BudgetRepository;
import com.banking.expenseplanner.util.UserUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

	@Autowired
	private BudgetRepository budgetRepository;

	@Autowired
	private UserUtil userUtil;

	@PostMapping("/add")
	public ResponseEntity<?> addBudget(@Valid @RequestBody Budget budget) {
		User user = userUtil.getLoggedInUser();
		budget.setUser(user);
		budgetRepository.save(budget);
		return ResponseEntity.ok("Budget added successfully");
	}

	@GetMapping("/my")
	public ResponseEntity<?> getUserBudgets(
	        @RequestParam(required = false) Integer month,
	        @RequestParam(required = false) Integer year) {

	    User user = userUtil.getLoggedInUser();

	    if (month != null && year != null) {
	        return ResponseEntity.ok(budgetRepository.findByUserAndMonthAndYear(user, month, year));
	    } else if (month != null) {
	        return ResponseEntity.ok(budgetRepository.findByUserAndMonth(user, month));
	    } else if (year != null) {
	        return ResponseEntity.ok(budgetRepository.findByUserAndYear(user, year));
	    } else {
	        return ResponseEntity.ok(budgetRepository.findByUser(user));
	    }
	}


	@GetMapping("/my/month/{month}/year/{year}")
	public ResponseEntity<?> getBudgetsForMonth(@PathVariable Integer month, @PathVariable Integer year) {
		User user = userUtil.getLoggedInUser();
		return ResponseEntity.ok(budgetRepository.findByUserAndMonthAndYear(user, month, year));
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateBudget(@PathVariable Long id, @Valid @RequestBody Budget updatedBudget) {
	    User user = userUtil.getLoggedInUser();
	    return budgetRepository.findById(id).map(budget -> {
	        if (!budget.getUser().getId().equals(user.getId())) {
	            return ResponseEntity.status(403).body("Access denied");
	        }
	        budget.setCategory(updatedBudget.getCategory());
	        budget.setLimitAmount(updatedBudget.getLimitAmount());
	        budget.setMonth(updatedBudget.getMonth());
	        budget.setYear(updatedBudget.getYear());
	        budgetRepository.save(budget);
	        return ResponseEntity.ok("Budget updated");
	    }).orElse(ResponseEntity.badRequest().body("Budget not found"));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
	    User user = userUtil.getLoggedInUser();
	    return budgetRepository.findById(id).map(budget -> {
	        if (!budget.getUser().getId().equals(user.getId())) {
	            return ResponseEntity.status(403).body("Access denied");
	        }
	        budgetRepository.deleteById(id);
	        return ResponseEntity.ok("Budget deleted");
	    }).orElse(ResponseEntity.badRequest().body("Budget not found"));
	}

}