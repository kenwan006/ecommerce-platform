package com.example.sunridge.warehouse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lightweight service-to-service authentication for Warehouse's internal HTTP API. */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
  private static final String HEADER = "X-Internal-Api-Key";
  private final byte[] expectedApiKey;

  public InternalApiKeyFilter(@Value("${internal.api-key}") String apiKey) {
    this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String provided = request.getHeader(HEADER);
    if (provided == null
        || !MessageDigest.isEqual(expectedApiKey, provided.getBytes(StandardCharsets.UTF_8))) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Internal service credential required");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
