package com.example.customers.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Micrometer metrics and monitoring. Enables collection and export of application
 * metrics for monitoring systems.
 */
@Configuration
public class MetricsConfig {

  /** Configure common tags for all metrics. Tags are added to all metrics to provide context. */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
      @Value("${spring.application.name}") String applicationName) {
    return registry ->
        registry
            .config()
            .commonTags(
                "application",
                applicationName,
                "environment",
                System.getProperty("spring.profiles.active", "unknown"));
  }
}
