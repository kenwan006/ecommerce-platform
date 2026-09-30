package com.example.shop.config;

import com.example.shop.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {
  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
      AuthenticationSuccessHandler googleAuthenticationSuccessHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${app.oauth.enabled:false}") boolean oauthEnabled) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(
                "/api/auth/register",
                "/api/auth/login",
                "/api/products/**",
                "/actuator/**",
                "/oauth2/**",
                "/login/oauth2/**",
                "/error"
            ).permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    if (oauthEnabled && clientRegistrationRepository.getIfAvailable() != null) {
      http.oauth2Login(oauth -> oauth.successHandler(googleAuthenticationSuccessHandler));
    }
    return http.build();
  }

  @Bean
  AuthenticationSuccessHandler googleAuthenticationSuccessHandler(
      AuthService authService,

      @Value("${app.frontend-url}") String frontendUrl) {

    return (request, response, authentication) -> {
      OAuth2User user = (OAuth2User) authentication.getPrincipal(); //get the OIDC ID token from Google OAuth2.0
      String subject = user.getAttribute("sub");
      String email = user.getAttribute("email");
      String name = user.getAttribute("name");
      Boolean emailVerified = user.getAttribute("email_verified");
      if (subject == null || email == null || name == null || !Boolean.TRUE.equals(emailVerified)) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google did not return the required profile information");
        return;
      }
      var localUser = authService.loginWithGoogle(subject, email, name);
      logger.info("Google sign-in succeeded for local user {}", localUser.get("userId"));
      var session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      response.sendRedirect(frontendUrl + "#access_token=" + localUser.get("accessToken"));
    };
  }
}
