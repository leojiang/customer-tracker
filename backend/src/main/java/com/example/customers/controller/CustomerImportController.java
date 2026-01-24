package com.example.customers.controller;

import com.example.customers.service.CustomerImportService;
import com.example.customers.service.CustomerImportService.ImportSummary;
import com.example.customers.service.CustomerImportService.StagingPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** REST controller for customer import operations. */
@Tag(name = "Customer Import", description = "APIs for bulk customer import via Excel")
@RestController
@RequestMapping("/api/customers/import")
@CrossOrigin(origins = "*")
public class CustomerImportController {

  private final CustomerImportService importService;

  @Autowired
  public CustomerImportController(CustomerImportService importService) {
    this.importService = importService;
  }

  /** Upload Excel file for staging. */
  @Operation(
      summary = "Upload Excel file",
      description =
          "Upload an Excel file with customer data. Data will be validated and staged for review.")
  @ApiResponse(responseCode = "200", description = "File uploaded and staged successfully")
  @ApiResponse(responseCode = "400", description = "Invalid file format")
  @PostMapping("/upload")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER')")
  public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
    // Validate file type
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(new UploadResponse(0, "File is empty"));
    }

    String filename = file.getOriginalFilename();
    if (filename == null || !filename.endsWith(".xlsx")) {
      return ResponseEntity.badRequest()
          .body(new UploadResponse(0, "Only .xlsx files are supported"));
    }

    try {
      int rowCount = importService.processUploadFile(file);
      return ResponseEntity.ok(
          new UploadResponse(rowCount, "Successfully processed " + rowCount + " rows"));

    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new UploadResponse(0, "Error processing file: " + e.getMessage()));
    }
  }

  /** Get all staged records for review with pagination. */
  @Operation(
      summary = "Get staged records",
      description = "Retrieve all staged customer records for review before import with pagination")
  @GetMapping("/staged")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER')")
  public ResponseEntity<StagingPageResponse> getStagedRecords(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
    StagingPageResponse response = importService.getStagedRecords(page, limit);
    return ResponseEntity.ok(response);
  }

  /** Get overall statistics for all staged records. */
  @Operation(
      summary = "Get staging statistics",
      description = "Retrieve overall statistics for all staged customer records grouped by status")
  @GetMapping("/statistics")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER')")
  public ResponseEntity<CustomerImportService.StagingStatistics> getStagingStatistics() {
    CustomerImportService.StagingStatistics statistics = importService.getStagingStatistics();
    return ResponseEntity.ok(statistics);
  }

  /** Confirm import and move staged data to customers table. */
  @Operation(
      summary = "Confirm import",
      description = "Confirm import and move all valid staged records to the customers table")
  @PostMapping("/confirm")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER')")
  public ResponseEntity<ImportSummary> confirmImport() {
    ImportSummary summary = importService.confirmImport();
    return ResponseEntity.ok(summary);
  }

  /** Cancel import and clear staged data. */
  @Operation(
      summary = "Cancel import",
      description = "Cancel import operation and clear all staged data")
  @DeleteMapping("/cancel")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER')")
  public ResponseEntity<Void> cancelImport() {
    importService.clearStaging();
    return ResponseEntity.noContent().build();
  }

  /** Response DTO for file upload. */
  public static class UploadResponse {
    private final int rowCount;
    private final String message;

    public UploadResponse(int rowCount, String message) {
      this.rowCount = rowCount;
      this.message = message;
    }

    public int getRowCount() {
      return rowCount;
    }

    public String getMessage() {
      return message;
    }
  }
}
