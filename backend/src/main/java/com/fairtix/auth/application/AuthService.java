package com.fairtix.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fairtix.users.domain.User;
import com.fairtix.users.dto.LoginRequest;
import com.fairtix.users.dto.RegisterRequest;
import com.fairtix.users.infrastructure.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public String register(RegisterRequest request) {
    if (userRepository.findByEmail(request.email()).isPresent()) {
      throw new RuntimeException("User already exists");
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(passwordEncoder.encode(request.password()));

    userRepository.save(user);

    return jwtService.generateToken(
        user.getId(),
        user.getEmail(),
        user.getRole());
  }

  public String login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new RuntimeException("Invalid credentials");
    }

    return jwtService.generateToken(
        user.getId(),
        user.getEmail(),
        user.getRole());
  }
}
