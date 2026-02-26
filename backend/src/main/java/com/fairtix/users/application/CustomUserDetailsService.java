package com.fairtix.users.application;

import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository repository;

  public CustomUserDetailsService(UserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String email)
      throws UsernameNotFoundException {

    User user = repository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return org.springframework.security.core.userdetails.User
        .builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .authorities("ROLE_" + user.getRole().name())
        .build();
  }
}
