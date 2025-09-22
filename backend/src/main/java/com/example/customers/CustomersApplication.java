package com.example.customers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class.
 *
 * <p>Entry point for the Customer Tracker application.
 */
@SpringBootApplication
public class CustomersApplication {
  public static void main(String[] args) {
    SpringApplication.run(CustomersApplication.class, args);
  }
}
