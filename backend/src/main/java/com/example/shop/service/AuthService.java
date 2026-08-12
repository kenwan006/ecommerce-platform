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
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository userRepository, JwtService jwtService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
  }

  @Transactional
  public Map<String, Object> register(AuthRequest request) {
    if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
      throw new IllegalStateException("Email is already registered");
    }

    User user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(encoder.encode(request.password()));
    userRepository.save(user);

    return response(user);
  }

  public Map<String, Object> login(LoginRequest request) {
    User user = userRepository
        .findByEmail(request.email().toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!encoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    return response(user);
  }

  @Transactional
  public Map<String, Object> loginWithGoogle(String subject, String email, String name) {
    User user = userRepository.findByOauthProviderAndOauthProviderSubject("google", subject)
        .orElseGet(() -> userRepository.findByEmail(email.toLowerCase()).orElseGet(User::new));

    user.setEmail(email);
    user.setName(name);
    user.setOauthProvider("google");
    user.setOauthProviderSubject(subject);
    userRepository.save(user);
    return response(user);
  }

  public Map<String, Object> currentUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return response(user);
  }

  private Map<String, Object> response(User user) {
    return Map.of(
        "userId", user.getId(),
        "name", user.getName(),
        "email", user.getEmail(),
        "accessToken", jwtService.createToken(user.getId()));
  }
}
