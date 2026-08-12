package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(unique = true)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "oauth_provider")
  private String oauthProvider;

  @Column(name = "oauth_provider_subject")
  private String oauthProviderSubject;

  private String role = "CUSTOMER";

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email.toLowerCase();
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getOauthProvider() { return oauthProvider; }
  public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
  public String getOauthProviderSubject() { return oauthProviderSubject; }
  public void setOauthProviderSubject(String oauthProviderSubject) { this.oauthProviderSubject = oauthProviderSubject; }
}
