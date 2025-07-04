// Reminder.java (Entity)
package com.banking.expenseplanner.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "reminders")
@Data
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Title is required")
    private String title;

    private String note;

    @NotNull(message = "Amount is required")
    private Double amount;


    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private boolean isRecurring = false;
}