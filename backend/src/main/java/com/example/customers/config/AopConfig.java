package com.example.customers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class to enable Aspect-Oriented Programming (AOP). Enables support for @Aspect
 * annotations and proxy-based AOP.
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
  // Configuration is handled by @EnableAspectJAutoProxy annotation
}
