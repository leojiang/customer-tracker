package com.example.customers.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for caching analytics and frequently accessed data. Uses Caffeine cache for
 * high-performance in-memory caching.
 *
 * <p>Cache is configured via application.yml with Spring Boot auto-configuration: - Type:
 * Caffeine - Maximum size: 100 entries - Expiration: 5 minutes after write
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /** Custom cache key generator for analytics queries. */
  @Bean
  public KeyGenerator keyGenerator() {
    return (target, method, params) -> {
      StringBuilder sb = new StringBuilder();
      sb.append(target.getClass().getSimpleName());
      sb.append(".");
      sb.append(method.getName());
      sb.append("(");

      for (Object param : params) {
        if (param != null) {
          sb.append(param.toString());
          sb.append(",");
        }
      }

      if (params.length > 0) {
        sb.deleteCharAt(sb.length() - 1);
      }

      sb.append(")");
      return sb.toString();
    };
  }

  /** Cache error handler - log errors but don't break the application. */
  @Bean
  public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
      @Override
      public void handleCacheGetError(
          RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
        // Log but don't throw - fail gracefully
        System.err.println("Cache get error in " + cache.getName() + ": " + exception.getMessage());
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception,
          org.springframework.cache.Cache cache,
          Object key,
          Object value) {
        System.err.println("Cache put error in " + cache.getName() + ": " + exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(
          RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
        System.err.println(
            "Cache evict error in " + cache.getName() + ": " + exception.getMessage());
      }

      @Override
      public void handleCacheClearError(
          RuntimeException exception, org.springframework.cache.Cache cache) {
        System.err.println(
            "Cache clear error in " + cache.getName() + ": " + exception.getMessage());
      }
    };
  }
}
