package com.example.shop.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final long EXPIRATION_SECONDS = 60 * 60;
  private final SecretKey signingKey;

  public JwtService(@Value("${app.jwt-secret}") String jwtSecret) {
    try {
      this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("app.jwt-secret must be a Base64-encoded secret of at least 32 bytes", exception);
    }
  }

  public String createToken(Long userId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
        .signWith(signingKey)
        .compact();
  }

  public Long getUserId(String token) {
    Claims claims = Jwts.parser().verifyWith(signingKey).build()
        .parseSignedClaims(token).getPayload();
    return Long.valueOf(claims.getSubject());
  }
}
