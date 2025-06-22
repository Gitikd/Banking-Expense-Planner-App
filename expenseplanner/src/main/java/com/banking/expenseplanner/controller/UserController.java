package com.banking.expenseplanner.controller;

import com.banking.expenseplanner.dto.LoginDto;
import com.banking.expenseplanner.dto.UserDto;
import com.banking.expenseplanner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String registerUser(@Valid @RequestBody UserDto userDto) {
        return userService.registerUser(userDto);
    }

    @PostMapping("/login")
    public String loginUser(@Valid @RequestBody LoginDto loginDto) {
        return userService.loginUser(loginDto);
    }
}
