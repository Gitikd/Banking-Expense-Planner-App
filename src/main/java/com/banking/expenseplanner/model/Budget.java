package com.banking.expenseplanner.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Data
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Limit amount is required")
    private Double limitAmount;

    @NotNull(message = "Month is required")
    private Integer month; // 1–12

    @NotNull(message = "Year is required")
    private Integer year;

    private LocalDate createdDate = LocalDate.now();
}
