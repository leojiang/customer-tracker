package com.example.customers.service;

import com.example.customers.dto.RefreshResult;
import com.example.customers.dto.ScriptResult;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsRefreshService {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsRefreshService.class);
  private static final String SCRIPTS_PATH = "db/migration/scripts";

  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * Refresh all analytical tables by running all backfill scripts Automatically discovers all .sql
   * files in the scripts folder
   */
  @Transactional
  public RefreshResult refreshAllAnalyticalTables() {
    log.info("Starting analytics data refresh at {}", LocalDateTime.now());
    long startTime = System.currentTimeMillis();

    RefreshResult result = new RefreshResult();
    result.setStartTime(LocalDateTime.now());
    List<ScriptResult> scriptResults = new ArrayList<>();

    try {
      // Discover all SQL scripts in the scripts folder
      List<String> scriptFiles = discoverBackfillScripts();
      log.info("Found {} backfill scripts to execute", scriptFiles.size());

      // Execute each script
      for (String scriptFile : scriptFiles) {
        ScriptResult scriptResult = executeBackfillScript(scriptFile);
        scriptResults.add(scriptResult);

        if (!scriptResult.getSuccess()) {
          log.error("Script {} failed: {}", scriptFile, scriptResult.getErrorMessage());
        }
      }

      // Calculate summary statistics
      int successfulCount = (int) scriptResults.stream().filter(ScriptResult::getSuccess).count();
      int failedCount = scriptResults.size() - successfulCount;

      result.setScriptResults(scriptResults);
      result.setTotalScriptsExecuted(scriptResults.size());
      result.setSuccessfulScripts(successfulCount);
      result.setFailedScripts(failedCount);
      result.setSuccess(failedCount == 0); // Overall success only if all scripts succeed

      long endTime = System.currentTimeMillis();
      result.setEndTime(LocalDateTime.now());
      result.setDurationMs(endTime - startTime);

      log.info(
          "Analytics data refresh completed in {} ms. "
              + "Scripts: {} total, {} successful, {} failed",
          result.getDurationMs(),
          scriptResults.size(),
          successfulCount,
          failedCount);

    } catch (Exception e) {
      log.error("Error during analytics data refresh", e);
      result.setSuccess(false);
      result.setErrorMessage(e.getMessage());
      result.setEndTime(LocalDateTime.now());
      result.setDurationMs(System.currentTimeMillis() - startTime);
    }

    return result;
  }

  /**
   * Update analytical tables for the last 12 months only. This protects historical data older than
   * 12 months from being overwritten.
   *
   * <p>This method only executes scripts starting with "update_", which are designed to update only
   * recent data while preserving historical records.
   *
   * @return RefreshResult with execution details
   */
  @Transactional
  public RefreshResult updateRecentAnalyticalTables() {
    log.info("Starting recent analytics update (last 12 months) at {}", LocalDateTime.now());
    long startTime = System.currentTimeMillis();

    RefreshResult result = new RefreshResult();
    result.setStartTime(LocalDateTime.now());
    List<ScriptResult> scriptResults = new ArrayList<>();

    try {
      // Discover only update scripts (starting with "update_")
      List<String> scriptFiles = discoverUpdateScripts();
      log.info("Found {} update scripts for last 12 months", scriptFiles.size());

      // Execute each script
      for (String scriptFile : scriptFiles) {
        ScriptResult scriptResult = executeBackfillScript(scriptFile);
        scriptResults.add(scriptResult);

        if (!scriptResult.getSuccess()) {
          log.error("Script {} failed: {}", scriptFile, scriptResult.getErrorMessage());
        }
      }

      // Calculate summary statistics
      int successfulCount = (int) scriptResults.stream().filter(ScriptResult::getSuccess).count();
      int failedCount = scriptResults.size() - successfulCount;

      result.setScriptResults(scriptResults);
      result.setTotalScriptsExecuted(scriptResults.size());
      result.setSuccessfulScripts(successfulCount);
      result.setFailedScripts(failedCount);
      result.setSuccess(failedCount == 0);

      long endTime = System.currentTimeMillis();
      result.setEndTime(LocalDateTime.now());
      result.setDurationMs(endTime - startTime);

      log.info(
          "Recent analytics update completed in {} ms. "
              + "Scripts: {} total, {} successful, {} failed",
          result.getDurationMs(),
          scriptResults.size(),
          successfulCount,
          failedCount);

    } catch (Exception e) {
      log.error("Error during recent analytics update", e);
      result.setSuccess(false);
      result.setErrorMessage(e.getMessage());
      result.setEndTime(LocalDateTime.now());
      result.setDurationMs(System.currentTimeMillis() - startTime);
    }

    return result;
  }

  /** Discover all .sql backfill scripts in the scripts folder */
  private List<String> discoverBackfillScripts() {
    try {
      org.springframework.core.io.Resource[] resources =
          new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
              .getResources("classpath:" + SCRIPTS_PATH + "/*.sql");

      return Arrays.stream(resources)
          .map(
              resource -> {
                try {
                  return resource.getFilename();
                } catch (Exception e) {
                  log.warn("Could not get filename for resource", e);
                  return null;
                }
              })
          .filter(name -> name != null && name.endsWith(".sql"))
          .sorted() // Execute scripts in alphabetical order
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Failed to discover backfill scripts", e);
      throw new RuntimeException("Could not discover backfill scripts", e);
    }
  }

  /** Discover only update scripts (starting with "update_") in the scripts folder */
  private List<String> discoverUpdateScripts() {
    try {
      org.springframework.core.io.Resource[] resources =
          new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
              .getResources("classpath:" + SCRIPTS_PATH + "/update_*.sql");

      return Arrays.stream(resources)
          .map(
              resource -> {
                try {
                  return resource.getFilename();
                } catch (Exception e) {
                  log.warn("Could not get filename for resource", e);
                  return null;
                }
              })
          .filter(name -> name != null && name.startsWith("update_") && name.endsWith(".sql"))
          .sorted() // Execute scripts in alphabetical order
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Failed to discover update scripts", e);
      throw new RuntimeException("Could not discover update scripts", e);
    }
  }

  /** Execute a single backfill script */
  private ScriptResult executeBackfillScript(String scriptFileName) {
    ScriptResult result = new ScriptResult(scriptFileName);
    List<String> executionLog = new ArrayList<>();
    long startTime = System.currentTimeMillis();

    try {
      log.info("Executing backfill script: {}", scriptFileName);
      executionLog.add("Loading script: " + scriptFileName);

      // Read the script file
      String scriptPath = SCRIPTS_PATH + "/" + scriptFileName;
      String scriptContent = readScriptFile(scriptPath);
      executionLog.add("Script loaded successfully (" + scriptContent.length() + " characters)");

      // Parse and execute SQL statements
      List<String> statements = parseSqlStatements(scriptContent);
      executionLog.add("Found " + statements.size() + " SQL statements to execute");

      int totalRecordsAffected = 0;
      for (int i = 0; i < statements.size(); i++) {
        String statement = statements.get(i);

        try {
          // Skip SELECT statements (they're for preview/verification only)
          if (statement.trim().toUpperCase().startsWith("SELECT")) {
            executionLog.add("Skipping SELECT statement " + (i + 1));
            continue;
          }

          // Execute the statement
          int updateCount = jdbcTemplate.update(statement);
          totalRecordsAffected += updateCount;
          executionLog.add(
              "Executed statement "
                  + (i + 1)
                  + ": "
                  + updateCount
                  + " rows affected - "
                  + truncateStatement(statement, 50));

        } catch (Exception e) {
          // Some statements might not return update count (like DROP, TRUNCATE)
          // Try executing with execute method for those
          try {
            jdbcTemplate.execute(statement);
            executionLog.add(
                "Executed statement " + (i + 1) + " - " + truncateStatement(statement, 50));
          } catch (Exception e2) {
            log.warn("Failed to execute statement {}: {}", i + 1, e2.getMessage());
            executionLog.add("Warning: Statement " + (i + 1) + " had issues: " + e2.getMessage());
          }
        }
      }

      result.setSuccess(true);
      result.setRecordsAffected(totalRecordsAffected);
      executionLog.add(
          "Script completed successfully. Total records affected: " + totalRecordsAffected);

    } catch (Exception e) {
      log.error("Failed to execute backfill script: {}", scriptFileName, e);
      result.setSuccess(false);
      result.setErrorMessage(e.getMessage());
      executionLog.add("ERROR: " + e.getMessage());
    }

    result.setExecutionLog(executionLog);
    result.setDurationMs(System.currentTimeMillis() - startTime);
    return result;
  }

  /** Read SQL script file from classpath */
  private String readScriptFile(String scriptPath) {
    try {
      ClassPathResource resource = new ClassPathResource(scriptPath);
      File file = resource.getFile();
      return new String(Files.readAllBytes(file.toPath()));
    } catch (Exception e) {
      log.error("Failed to read script file: {}", scriptPath, e);
      throw new RuntimeException("Could not read script file: " + scriptPath, e);
    }
  }

  /**
   * Parse SQL content into individual statements Splits by semicolon but handles multi-line
   * statements and comments
   */
  private List<String> parseSqlStatements(String sqlContent) {
    List<String> statements = new ArrayList<>();
    StringBuilder currentStatement = new StringBuilder();
    String[] lines = sqlContent.split("\n");

    for (String line : lines) {
      String trimmed = line.trim();

      // Skip empty lines and comment-only lines
      if (trimmed.isEmpty()) {
        continue;
      }

      // Skip full-line comments
      if (trimmed.startsWith("--")) {
        continue;
      }

      // Remove inline comments
      int commentIndex = trimmed.indexOf("--");
      if (commentIndex > 0) {
        trimmed = trimmed.substring(0, commentIndex).trim();
      }

      // Append to current statement
      if (currentStatement.length() > 0) {
        currentStatement.append(" ");
      }
      currentStatement.append(trimmed);

      // Check if statement ends with semicolon
      if (trimmed.endsWith(";")) {
        String statement = currentStatement.toString();
        // Remove trailing semicolon
        statement = statement.substring(0, statement.length() - 1).trim();
        if (!statement.isEmpty()) {
          statements.add(statement);
        }
        currentStatement = new StringBuilder();
      }
    }

    // Handle last statement without semicolon
    if (currentStatement.length() > 0) {
      String statement = currentStatement.toString().trim();
      if (!statement.isEmpty()) {
        statements.add(statement);
      }
    }

    return statements;
  }

  /** Truncate statement for logging */
  private String truncateStatement(String statement, int maxLength) {
    if (statement == null) {
      return "";
    }
    statement = statement.replaceAll("\\s+", " ").trim();
    if (statement.length() > maxLength) {
      return statement.substring(0, maxLength) + "...";
    }
    return statement;
  }
}
