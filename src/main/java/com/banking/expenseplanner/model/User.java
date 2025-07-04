package com.banking.expenseplanner.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data  // Generates getters, setters, toString, equals, hashCode using Lombok
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremented ID
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Double balance = 0.0;  // Default balance


}
