package com.example.customers.dto;

import java.util.List;

public class ScriptResult {
  private String scriptName;
  private Boolean success;
  private String errorMessage;
  private Long durationMs;
  private Integer recordsAffected;
  private List<String> executionLog;

  public ScriptResult() {}

  public ScriptResult(String scriptName) {
    this.scriptName = scriptName;
    this.success = true;
  }

  public String getScriptName() {
    return scriptName;
  }

  public void setScriptName(String scriptName) {
    this.scriptName = scriptName;
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

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public Integer getRecordsAffected() {
    return recordsAffected;
  }

  public void setRecordsAffected(Integer recordsAffected) {
    this.recordsAffected = recordsAffected;
  }

  public List<String> getExecutionLog() {
    return executionLog;
  }

  public void setExecutionLog(List<String> executionLog) {
    this.executionLog = executionLog;
  }
}
