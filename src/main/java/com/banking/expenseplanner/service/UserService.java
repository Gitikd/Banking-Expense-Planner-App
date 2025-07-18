package com.banking.expenseplanner.service;

import com.banking.expenseplanner.dto.LoginDto;
import com.banking.expenseplanner.dto.UserDto;
import com.banking.expenseplanner.model.User;
import com.banking.expenseplanner.repository.UserRepository;
import com.banking.expenseplanner.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	// ✅ Registration logic
	public String registerUser(UserDto userDto) {
		Optional<User> existingUser = userRepository.findByEmail(userDto.getEmail());

		if (existingUser.isPresent()) {
			return "Email already exists!";
		}

		User user = new User();
		user.setName(userDto.getName());
		user.setEmail(userDto.getEmail());
		user.setPassword(passwordEncoder.encode(userDto.getPassword()));

		userRepository.save(user);
		return "User registered successfully!";
	}

	// ✅ Login logic
	public String loginUser(LoginDto loginDto) {
		Optional<User> userOpt = userRepository.findByEmail(loginDto.getEmail());

		if (userOpt.isPresent()) {
			User user = userOpt.get();
			if (passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
				return "Bearer " + jwtUtil.generateToken(user.getEmail());
			} else {
				return "Invalid password!";
			}
		} else {
			return "User not found!";
		}
	}

	// ✅ For internal UI support if needed
	public boolean registerUserUI(UserDto userDto) {
		Optional<User> existingUser = userRepository.findByEmail(userDto.getEmail());
		if (existingUser.isPresent())
			return false;

		User user = new User();
		user.setName(userDto.getName());
		user.setEmail(userDto.getEmail());
		user.setPassword(userDto.getPassword()); // You may optionally encode here too
		userRepository.save(user);
		return true;
	}

	public boolean loginUserUI(UserDto userDto) {
		Optional<User> userOpt = userRepository.findByEmail(userDto.getEmail());
		return userOpt.isPresent() && userOpt.get().getPassword().equals(userDto.getPassword());
	}

}
