package com.example.customers.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RefreshResult {
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Long durationMs;
  private Boolean success;
  private String errorMessage;
  private List<ScriptResult> scriptResults;
  private Integer totalScriptsExecuted;
  private Integer successfulScripts;
  private Integer failedScripts;

  // Getters and Setters
  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public List<ScriptResult> getScriptResults() {
    return scriptResults;
  }

  public void setScriptResults(List<ScriptResult> scriptResults) {
    this.scriptResults = scriptResults;
  }

  public Integer getTotalScriptsExecuted() {
    return totalScriptsExecuted;
  }

  public void setTotalScriptsExecuted(Integer totalScriptsExecuted) {
    this.totalScriptsExecuted = totalScriptsExecuted;
  }

  public Integer getSuccessfulScripts() {
    return successfulScripts;
  }

  public void setSuccessfulScripts(Integer successfulScripts) {
    this.successfulScripts = successfulScripts;
  }

  public Integer getFailedScripts() {
    return failedScripts;
  }

  public void setFailedScripts(Integer failedScripts) {
    this.failedScripts = failedScripts;
  }
}
