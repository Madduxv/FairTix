package com.fairtix.auth.api;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairtix.auth.application.AuthService;
import com.fairtix.users.dto.LoginRequest;
import com.fairtix.users.dto.RegisterRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  public Map<String, String> register(@RequestBody RegisterRequest request) {
    return Map.of("token", service.register(request));
  }

  @PostMapping("/login")
  public Map<String, String> login(@RequestBody LoginRequest request) {
    return Map.of("token", service.login(request));
  }
}
