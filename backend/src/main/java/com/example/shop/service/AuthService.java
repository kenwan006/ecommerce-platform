package com.example.shop.service;

import com.example.shop.dto.AuthRequest;
import com.example.shop.dto.LoginRequest;
import com.example.shop.entity.User;
import com.example.shop.repository.UserRepository;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
  private final UserRepository users;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository users) {
    this.users = users;
  }

  @Transactional
  public Map<String, Object> register(AuthRequest request) {
    if (users.findByEmail(request.email().toLowerCase()).isPresent()) {
      throw new IllegalStateException("Email is already registered");
    }

    User user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(encoder.encode(request.password()));
    users.save(user);

    return response(user);
  }

  public Map<String, Object> login(LoginRequest request) {
    User user = users
        .findByEmail(request.email().toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!encoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    return response(user);
  }

  private Map<String, Object> response(User user) {
    return Map.of(
        "userId", user.getId(),
        "name", user.getName(),
        "email", user.getEmail());
  }
}
