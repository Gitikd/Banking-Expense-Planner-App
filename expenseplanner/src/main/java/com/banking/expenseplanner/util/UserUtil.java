package com.banking.expenseplanner.util;

import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserUtil {

	private final UserRepository userRepository;

	public UserUtil(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getLoggedInUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
	}
}
