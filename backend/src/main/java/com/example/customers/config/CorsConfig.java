package com.example.customers.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for cross-origin requests.
 *
 * <p>Configures Cross-Origin Resource Sharing settings for the API and Swagger UI.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://47.109.72.216:*",
            "http://47.109.72.216",
            "http://47.109.72.216:80")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .exposedHeaders("*")
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

  /**
   * Creates CORS configuration source for Spring Security.
   *
   * @return CorsConfigurationSource configured CORS source
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://47.109.72.216:*",
            "http://47.109.72.216",  // For nginx on port 80 (implicit port)
            "http://47.109.72.216:80"));  // Explicit port 80
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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
