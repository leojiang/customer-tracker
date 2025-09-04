package com.example.customers.controller;

import com.example.customers.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health Check", description = "System health and monitoring endpoints")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CustomerService customerService;

    @Operation(
        summary = "Comprehensive health check",
        description = "Returns detailed system health including database connectivity and basic statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System is healthy"),
        @ApiResponse(responseCode = "500", description = "System has issues")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                health.put("database", "UP");
                health.put("databaseUrl", connection.getMetaData().getURL());
            }

            // Get basic statistics
            CustomerService.CustomerStatistics stats = customerService.getCustomerStatistics(false);
            health.put("totalCustomers", stats.getTotalCustomers());
            health.put("recentlyUpdated", stats.getRecentlyUpdatedCount());

            health.put("status", "UP");
            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<String> simpleHealth() {
        return ResponseEntity.ok("OK");
    }
}
