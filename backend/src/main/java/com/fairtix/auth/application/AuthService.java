package com.fairtix.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fairtix.users.domain.User;
import com.fairtix.users.dto.LoginRequest;
import com.fairtix.users.dto.RegisterRequest;
import com.fairtix.users.infrastructure.UserRepository;

import java.util.regex.Pattern;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginAttemptService loginAttemptService;
  private final RecaptchaService recaptchaService;

  private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
  private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
  private static final Pattern DIGIT = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]");
  private static final int MIN_PASSWORD_LENGTH = 8;

  public AuthService(UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginAttemptService loginAttemptService,
      RecaptchaService recaptchaService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginAttemptService = loginAttemptService;
    this.recaptchaService = recaptchaService;
  }

  public String register(RegisterRequest request) {
    validatePasswordStrength(request.password());

    if (userRepository.findByEmail(request.email()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(passwordEncoder.encode(request.password()));

    userRepository.save(user);

    return jwtService.generateToken(
        user.getId(),
        user.getEmail(),
        user.getRole().name());
  }

  public String login(LoginRequest request) {
    String email = request.email();

    // Check lockout before anything else
    if (loginAttemptService.isLocked(email)) {
      throw new AccountLockedException(loginAttemptService.getRemainingLockoutSeconds(email));
    }

    // After 3 failures, captcha is required; validate before password check
    long failedAttempts = loginAttemptService.getAttemptCount(email);
    if (recaptchaService.isCaptchaRequired(failedAttempts)) {
      recaptchaService.assertValidToken(request.recaptchaToken());
    }

    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null || user.isDeleted()
        || !passwordEncoder.matches(request.password(), user.getPassword())) {
      loginAttemptService.recordFailure(email);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // Successful login -- reset attempts
    loginAttemptService.resetAttempts(email);

    return jwtService.generateToken(
        user.getId(),
        user.getEmail(),
        user.getRole().name());
  }

  private void validatePasswordStrength(String password) {
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new WeakPasswordException(
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
    }
    if (!UPPERCASE.matcher(password).find()) {
      throw new WeakPasswordException("Password must contain at least one uppercase letter");
    }
    if (!LOWERCASE.matcher(password).find()) {
      throw new WeakPasswordException("Password must contain at least one lowercase letter");
    }
    if (!DIGIT.matcher(password).find()) {
      throw new WeakPasswordException("Password must contain at least one digit");
    }
    if (!SPECIAL.matcher(password).find()) {
      throw new WeakPasswordException("Password must contain at least one special character");
    }
  }
}
