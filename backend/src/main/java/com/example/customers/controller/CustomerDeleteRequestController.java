package com.example.customers.controller;

import com.example.customers.model.CustomerDeleteRequest;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.service.CustomerDeleteRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer delete request operations.
 *
 * <p>Provides endpoints for officers to request customer deletion and admins to approve/reject
 * requests.
 */
@Tag(
    name = "Customer Delete Request Management",
    description = "APIs for managing customer deletion requests")
@RestController
@RequestMapping("/api/customer-delete-requests")
@CrossOrigin(origins = "*")
public class CustomerDeleteRequestController {

  private final CustomerDeleteRequestService deleteRequestService;

  /**
   * Constructor for CustomerDeleteRequestController.
   *
   * @param deleteRequestService service for delete request operations
   */
  @Autowired
  public CustomerDeleteRequestController(CustomerDeleteRequestService deleteRequestService) {
    this.deleteRequestService = deleteRequestService;
  }

  /** Request record class for creating delete requests. */
  @Schema(description = "Request to delete a customer")
  public static class DeleteRequestRequest {
    @Schema(description = "ID of the customer to delete", required = true)
    private UUID customerId;

    @Schema(description = "Reason for deletion", required = true)
    @NotBlank(message = "Reason is required")
    private String reason;

    public UUID getCustomerId() {
      return customerId;
    }

    public void setCustomerId(UUID customerId) {
      this.customerId = customerId;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  /** Reject request record class. */
  @Schema(description = "Request to reject a delete request")
  public static class RejectRequestRequest {
    @Schema(description = "Reason for rejection", required = true)
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    public String getRejectionReason() {
      return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
      this.rejectionReason = rejectionReason;
    }
  }

  /** Approve request record class. */
  @Schema(description = "Request to approve a delete request")
  public static class ApproveRequestRequest {
    @Schema(description = "Reason for approval (optional)")
    private String reason;

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  @Operation(
      summary = "Create a delete request for a customer",
      description =
          "Admins and Customer Agents can request customer deletion. Admins will review and approve/reject.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Delete request created successfully",
            content = @Content(schema = @Schema(implementation = CustomerDeleteRequest.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or pending request exists"),
        @ApiResponse(responseCode = "403", description = "User not authorized to create requests"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
      })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER_AGENT')")
  public ResponseEntity<?> createDeleteRequest(
      @RequestBody DeleteRequestRequest request, Authentication authentication) {

    Sales requester = (Sales) authentication.getPrincipal();

    // Admins and Customer Agents can create delete requests
    if (requester.getRole() != SalesRole.ADMIN && requester.getRole() != SalesRole.CUSTOMER_AGENT) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    try {
      CustomerDeleteRequest deleteRequest =
          deleteRequestService.createDeleteRequest(
              request.getCustomerId(), requester, request.getReason());
      return ResponseEntity.status(HttpStatus.CREATED).body(toDto(deleteRequest));
    } catch (IllegalStateException | jakarta.persistence.EntityNotFoundException e) {
      // Return error message as JSON
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(
      summary = "Get pending delete requests",
      description = "Retrieve pending customer delete requests with pagination")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending requests"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping("/pending")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Page<CustomerDeleteRequestDto>> getPendingRequests(
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int pageSize,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort =
        "asc".equalsIgnoreCase(sortDir)
            ? Sort.by("createdAt").ascending()
            : Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, pageSize, sort);

    Page<CustomerDeleteRequest> requests = deleteRequestService.getPendingRequests(pageable);
    Page<CustomerDeleteRequestDto> dtoPage = requests.map(this::toDto);
    return ResponseEntity.ok(dtoPage);
  }

  @Operation(
      summary = "Approve a delete request",
      description = "Admins can approve delete requests, which will delete the customer")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Delete request approved and customer deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot approve non-pending request"),
        @ApiResponse(responseCode = "403", description = "User not authorized"),
        @ApiResponse(responseCode = "404", description = "Delete request not found")
      })
  @PatchMapping("/{requestId}/approve")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> approveDeleteRequest(
      @PathVariable UUID requestId,
      @RequestBody ApproveRequestRequest request,
      Authentication authentication) {

    Sales admin = (Sales) authentication.getPrincipal();

    try {
      deleteRequestService.approveDeleteRequest(requestId, admin.getPhone(), request.getReason());
      return ResponseEntity.noContent().build();
    } catch (jakarta.persistence.EntityNotFoundException | IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Reject a delete request",
      description = "Admins can reject delete requests with a reason")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Delete request rejected"),
        @ApiResponse(responseCode = "400", description = "Cannot reject non-pending request"),
        @ApiResponse(responseCode = "403", description = "User not authorized"),
        @ApiResponse(responseCode = "404", description = "Delete request not found")
      })
  @PatchMapping("/{requestId}/reject")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> rejectDeleteRequest(
      @PathVariable UUID requestId,
      @RequestBody RejectRequestRequest request,
      Authentication authentication) {

    Sales admin = (Sales) authentication.getPrincipal();

    try {
      deleteRequestService.rejectDeleteRequest(
          requestId, admin.getPhone(), request.getRejectionReason());
      return ResponseEntity.noContent().build();
    } catch (jakarta.persistence.EntityNotFoundException | IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Get delete request by ID",
      description = "Retrieve a specific delete request")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved delete request"),
        @ApiResponse(responseCode = "404", description = "Delete request not found")
      })
  @GetMapping("/{requestId}")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<CustomerDeleteRequest> getDeleteRequest(@PathVariable UUID requestId) {
    try {
      CustomerDeleteRequest deleteRequest = deleteRequestService.getDeleteRequest(requestId);
      return ResponseEntity.ok(deleteRequest);
    } catch (jakarta.persistence.EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(
      summary = "Get all delete requests with status filter",
      description = "Retrieve delete requests filtered by status with pagination")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved delete requests"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Page<CustomerDeleteRequestDto>> getAllDeleteRequests(
      @Parameter(description = "Filter by status (PENDING, APPROVED, REJECTED, or ALL)")
          @RequestParam(defaultValue = "ALL")
          String status,
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int pageSize,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort =
        "asc".equalsIgnoreCase(sortDir)
            ? Sort.by("createdAt").ascending()
            : Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, pageSize, sort);

    Page<CustomerDeleteRequest> requests =
        deleteRequestService.getAllDeleteRequests(status, pageable);
    Page<CustomerDeleteRequestDto> dtoPage = requests.map(this::toDto);
    return ResponseEntity.ok(dtoPage);
  }

  @Operation(
      summary = "Count pending delete requests",
      description = "Get count of pending requests")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully counted pending requests"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping("/pending/count")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Long> countPendingRequests() {
    long count = deleteRequestService.countPendingRequests();
    return ResponseEntity.ok(count);
  }

  @Operation(summary = "Get delete request statistics", description = "Get counts by status")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping("/statistics")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<DeleteRequestStatistics> getStatistics() {
    DeleteRequestStatistics stats = deleteRequestService.getStatistics();
    return ResponseEntity.ok(stats);
  }

  /**
   * Convert CustomerDeleteRequest entity to DTO.
   *
   * @param request the entity to convert
   * @return the DTO with customer information
   */
  private CustomerDeleteRequestDto toDto(CustomerDeleteRequest request) {
    String customerId = null;
    try {
      if (request.getCustomer() != null) {
        customerId = request.getCustomer().getId().toString();
      }
    } catch (Exception e) {
      // Customer might be soft deleted and not accessible through lazy loading
      // We'll use the snapshot fields instead
    }

    return new CustomerDeleteRequestDto(
        request.getId().toString(),
        customerId,
        request.getCustomerName(),
        request.getCustomerPhone(),
        request.getRequestedBy().getPhone(),
        request.getRequestStatus().name(),
        request.getReason(),
        request.getCreatedAt().toString(),
        request.getReviewedBy(),
        request.getReviewedAt() != null ? request.getReviewedAt().toString() : null,
        request.getRejectionReason());
  }

  /** DTO for customer delete request responses. */
  public static class CustomerDeleteRequestDto {
    private String id;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String requestedBy;
    private String requestStatus;
    private String reason;
    private String createdAt;
    private String reviewedBy;
    private String reviewedAt;
    private String rejectionReason;

    public CustomerDeleteRequestDto(
        String id,
        String customerId,
        String customerName,
        String customerPhone,
        String requestedBy,
        String requestStatus,
        String reason,
        String createdAt,
        String reviewedBy,
        String reviewedAt,
        String rejectionReason) {
      this.id = id;
      this.customerId = customerId;
      this.customerName = customerName;
      this.customerPhone = customerPhone;
      this.requestedBy = requestedBy;
      this.requestStatus = requestStatus;
      this.reason = reason;
      this.createdAt = createdAt;
      this.reviewedBy = reviewedBy;
      this.reviewedAt = reviewedAt;
      this.rejectionReason = rejectionReason;
    }

    // Getters
    public String getId() {
      return id;
    }

    public String getCustomerId() {
      return customerId;
    }

    public String getCustomerName() {
      return customerName;
    }

    public String getCustomerPhone() {
      return customerPhone;
    }

    public String getRequestedBy() {
      return requestedBy;
    }

    public String getRequestStatus() {
      return requestStatus;
    }

    public String getReason() {
      return reason;
    }

    public String getCreatedAt() {
      return createdAt;
    }

    public String getReviewedBy() {
      return reviewedBy;
    }

    public String getReviewedAt() {
      return reviewedAt;
    }

    public String getRejectionReason() {
      return rejectionReason;
    }
  }

  /** Statistics for customer delete requests. */
  public static class DeleteRequestStatistics {
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private double approvalRate;

    public DeleteRequestStatistics(
        long pendingCount, long approvedCount, long rejectedCount, double approvalRate) {
      this.pendingCount = pendingCount;
      this.approvedCount = approvedCount;
      this.rejectedCount = rejectedCount;
      this.approvalRate = approvalRate;
    }

    public long getPendingCount() {
      return pendingCount;
    }

    public long getApprovedCount() {
      return approvedCount;
    }

    public long getRejectedCount() {
      return rejectedCount;
    }

    public double getApprovalRate() {
      return approvalRate;
    }
  }
}
