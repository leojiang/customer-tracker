package com.example.customers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for performance logging thresholds. These can be overridden in
 * application.yml per environment.
 */
@Configuration
@ConfigurationProperties(prefix = "logging.performance")
public class LoggingPerformanceProperties {

  /** Whether performance logging is enabled. Default: true */
  private boolean enabled = true;

  /**
   * Threshold for logging slow database operations (in milliseconds). Default: 1000ms (1 second)
   */
  private long databaseThreshold = 1000;

  /** Threshold for logging slow service methods (in milliseconds). Default: 2000ms (2 seconds) */
  private long serviceThreshold = 2000;

  /**
   * Threshold for logging slow controller methods (in milliseconds). Default: 3000ms (3 seconds)
   */
  private long controllerThreshold = 3000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getDatabaseThreshold() {
    return databaseThreshold;
  }

  public void setDatabaseThreshold(long databaseThreshold) {
    this.databaseThreshold = databaseThreshold;
  }

  public long getServiceThreshold() {
    return serviceThreshold;
  }

  public void setServiceThreshold(long serviceThreshold) {
    this.serviceThreshold = serviceThreshold;
  }

  public long getControllerThreshold() {
    return controllerThreshold;
  }

  public void setControllerThreshold(long controllerThreshold) {
    this.controllerThreshold = controllerThreshold;
  }
}
