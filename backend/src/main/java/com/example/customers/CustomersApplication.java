package com.example.customers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class.
 *
 * <p>Entry point for the Customer Tracker application.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CustomersApplication {
  public static void main(String[] args) {
    SpringApplication.run(CustomersApplication.class, args);
  }
}
