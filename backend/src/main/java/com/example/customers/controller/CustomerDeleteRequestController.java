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

  /**
   * Request record class for creating delete requests.
   */
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

  /**
   * Reject request record class.
   */
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

  @Operation(
      summary = "Create a delete request for a customer",
      description = "Officers can request customer deletion. Admins will review and approve/reject.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Delete request created successfully",
            content = @Content(schema = @Schema(implementation = CustomerDeleteRequest.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or pending request exists"),
        @ApiResponse(responseCode = "403", description = "User not authorized to create requests"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
      })
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
  public ResponseEntity<CustomerDeleteRequest> createDeleteRequest(
      @RequestBody DeleteRequestRequest request, Authentication authentication) {

    Sales requester = (Sales) authentication.getPrincipal();

    // Only Officers can create delete requests (Admins can delete directly)
    if (requester.getRole() != SalesRole.OFFICER && requester.getRole() != SalesRole.ADMIN) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    try {
      CustomerDeleteRequest deleteRequest =
          deleteRequestService.createDeleteRequest(
              request.getCustomerId(), requester, request.getReason());
      return ResponseEntity.status(HttpStatus.CREATED).body(deleteRequest);
    } catch (IllegalStateException | jakarta.persistence.EntityNotFoundException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Get pending delete requests",
      description = "Retrieve pending customer delete requests with pagination")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved pending requests"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping("/pending")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<CustomerDeleteRequest>> getPendingRequests(
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int pageSize,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort =
        "asc".equalsIgnoreCase(sortDir) ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, pageSize, sort);

    Page<CustomerDeleteRequest> requests = deleteRequestService.getPendingRequests(pageable);
    return ResponseEntity.ok(requests);
  }

  @Operation(
      summary = "Approve a delete request",
      description = "Admins can approve delete requests, which will delete the customer")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Delete request approved and customer deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot approve non-pending request"),
        @ApiResponse(responseCode = "403", description = "User not authorized"),
        @ApiResponse(responseCode = "404", description = "Delete request not found")
      })
  @PatchMapping("/{requestId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> approveDeleteRequest(
      @PathVariable UUID requestId, Authentication authentication) {

    Sales admin = (Sales) authentication.getPrincipal();

    try {
      deleteRequestService.approveDeleteRequest(requestId, admin.getPhone());
      return ResponseEntity.noContent().build();
    } catch (jakarta.persistence.EntityNotFoundException |
             IllegalStateException e) {
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
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> rejectDeleteRequest(
      @PathVariable UUID requestId,
      @RequestBody RejectRequestRequest request,
      Authentication authentication) {

    Sales admin = (Sales) authentication.getPrincipal();

    try {
      deleteRequestService.rejectDeleteRequest(
          requestId, admin.getPhone(), request.getRejectionReason());
      return ResponseEntity.noContent().build();
    } catch (jakarta.persistence.EntityNotFoundException |
             IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(summary = "Get delete request by ID", description = "Retrieve a specific delete request")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved delete request"),
        @ApiResponse(responseCode = "404", description = "Delete request not found")
      })
  @GetMapping("/{requestId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CustomerDeleteRequest> getDeleteRequest(@PathVariable UUID requestId) {
    try {
      CustomerDeleteRequest deleteRequest = deleteRequestService.getDeleteRequest(requestId);
      return ResponseEntity.ok(deleteRequest);
    } catch (jakarta.persistence.EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(summary = "Count pending delete requests", description = "Get count of pending requests")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successfully counted pending requests"),
        @ApiResponse(responseCode = "403", description = "User not authorized")
      })
  @GetMapping("/pending/count")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Long> countPendingRequests() {
    long count = deleteRequestService.countPendingRequests();
    return ResponseEntity.ok(count);
  }
}
