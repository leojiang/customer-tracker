package com.example.customers.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);

    // Allow CORS for Swagger UI
    registry
        .addMapping("/swagger-ui/**")
        .allowedOrigins("*")
        .allowedMethods("GET")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);

    registry
        .addMapping("/v3/api-docs/**")
        .allowedOrigins("*")
        .allowedMethods("GET")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    // Configuration for Swagger UI (allow all origins for documentation)
    CorsConfiguration swaggerConfiguration = new CorsConfiguration();
    swaggerConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
    swaggerConfiguration.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
    swaggerConfiguration.setAllowedHeaders(Arrays.asList("*"));
    swaggerConfiguration.setAllowCredentials(false);
    swaggerConfiguration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    source.registerCorsConfiguration("/swagger-ui/**", swaggerConfiguration);
    source.registerCorsConfiguration("/v3/api-docs/**", swaggerConfiguration);
    source.registerCorsConfiguration("/swagger-resources/**", swaggerConfiguration);
    source.registerCorsConfiguration("/webjars/**", swaggerConfiguration);
    return source;
  }
}
