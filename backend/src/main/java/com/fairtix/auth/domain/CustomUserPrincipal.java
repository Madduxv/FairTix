package com.fairtix.auth.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

public class CustomUserPrincipal extends User {

  private final UUID userId;

  public CustomUserPrincipal(UUID userId, String email, String password,
      Collection<? extends GrantedAuthority> authorities) {
    super(email, password, authorities);
    this.userId = userId;
  }

  public UUID getUserId() {
    return userId;
  }
}
