package com.example.customers.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Customer Tracker API")
                .description(
                    "A comprehensive Customer Call-Through and Status Tracking system for sales teams. "
                        + "Manage customer information, track status transitions, and maintain complete audit trails.")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("Customer Tracker Team")
                        .email("support@customertracker.com")
                        .url("https://github.com/example/customer-tracker"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Development Server"),
                new Server()
                    .url("https://api.customertracker.com")
                    .description("Production Server")));
  }
}
