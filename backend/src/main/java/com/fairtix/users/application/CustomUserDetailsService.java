package com.fairtix.users.application;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

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

    return new CustomUserPrincipal(
        user.getId(),
        user.getEmail(),
        user.getPassword(),
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
  }
}
