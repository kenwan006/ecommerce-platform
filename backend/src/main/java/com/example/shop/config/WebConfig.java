package com.example.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
  @Bean
  WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            // 18080 is the local end of the private SSM tunnel used to test
            // the deployed frontend without opening EC2 to the internet.
            .allowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:18080")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowCredentials(true);
      }
    };
  }
}
