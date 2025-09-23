package com.example.customers.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Temporary controller to check Flyway migration status. */
@RestController
@RequestMapping("/api/flyway")
public class FlywayController {

  @Autowired private JdbcTemplate jdbcTemplate;

  @GetMapping("/history")
  public List<Map<String, Object>> getFlywayHistory() {
    try {
      return jdbcTemplate.queryForList(
          "SELECT * FROM flyway_schema_history ORDER BY installed_rank");
    } catch (Exception e) {
      return List.of(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/status")
  public Map<String, Object> getFlywayStatus() {
    try {
      List<Map<String, Object>> history =
          jdbcTemplate.queryForList(
              "SELECT COUNT(*) as total_migrations FROM flyway_schema_history");
      return Map.of(
          "status", "success",
          "total_migrations", history.get(0).get("total_migrations"),
          "message", "Flyway history table accessible");
    } catch (Exception e) {
      return Map.of(
          "status", "error",
          "error", e.getMessage(),
          "message", "Cannot access Flyway history table");
    }
  }
}
