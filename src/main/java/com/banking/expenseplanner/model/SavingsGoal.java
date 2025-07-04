package com.banking.expenseplanner.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Table(name = "savings_goals")
@Data
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Goal name is required")
    private String goalName;

    @NotBlank(message = "Description is required")
    private String description;

    @Column(name = "status") // values: PENDING, COMPLETED, PURCHASED
    private String status = "PENDING";

    @NotNull(message = "Target amount is required")
    private Double targetAmount;

    private Double savedAmount = 0.0;
}
