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
            // Cloudflare Quick Tunnels give development deployments a temporary
            // HTTPS origin. This pattern avoids rebuilding the service whenever
            // Cloudflare assigns the next random tunnel hostname.
            .allowedOriginPatterns("https://*.trycloudflare.com")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowCredentials(true);
      }
    };
  }
}
