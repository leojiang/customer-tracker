package com.example.customers.controller;

import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.model.UserApprovalHistory;
import com.example.customers.service.UserApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user approval management operations.
 *
 * <p>Provides admin endpoints for managing user registration approvals.
 */
@Tag(
    name = "User Approval Management",
    description = "Admin endpoints for managing user registration approvals")
@RestController
@RequestMapping("/api/admin/user-approvals")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('ADMIN')")
public class UserApprovalController {

  private final UserApprovalService approvalService;

  @Autowired
  public UserApprovalController(UserApprovalService approvalService) {
    this.approvalService = approvalService;
  }

  @Operation(
      summary = "Get user approvals",
      description = "Retrieve paginated list of users by approval status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user approvals",
            content = @Content(schema = @Schema(implementation = ApprovalPageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
      })
  @GetMapping
  public ResponseEntity<ApprovalPageResponse> getUserApprovals(
      @Parameter(description = "Approval status filter") @RequestParam(defaultValue = "PENDING")
          ApprovalStatus status,
      @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20")
          int limit) {

    // Validate pagination parameters
    if (page < 1) page = 1;
    if (limit < 1) limit = 20;
    if (limit > 100) limit = 100;

    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

    Page<Sales> users = approvalService.getUsersByApprovalStatus(status, pageable);

    List<UserApprovalDto> userDtos =
        users.getContent().stream().map(this::toApprovalDto).collect(Collectors.toList());

    ApprovalPageResponse response =
        new ApprovalPageResponse(
            userDtos, users.getTotalElements(), page, limit, users.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get all users",
      description = "Retrieve paginated list of all users for management")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all users",
            content = @Content(schema = @Schema(implementation = ApprovalPageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
      })
  @GetMapping("/all-users")
  public ResponseEntity<ApprovalPageResponse> getAllUsers(
      @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20")
          int limit) {

    // Validate pagination parameters
    if (page < 1) page = 1;
    if (limit < 1) limit = 20;
    if (limit > 100) limit = 100;

    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

    Page<Sales> users = approvalService.getAllUsers(pageable);

    List<UserApprovalDto> userDtos =
        users.getContent().stream().map(this::toApprovalDto).collect(Collectors.toList());

    ApprovalPageResponse response =
        new ApprovalPageResponse(
            userDtos, users.getTotalElements(), page, limit, users.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get approved users by enabled status",
      description = "Retrieve paginated list of approved users filtered by enabled/disabled status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved users",
            content = @Content(schema = @Schema(implementation = ApprovalPageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
      })
  @GetMapping("/approved-users")
  public ResponseEntity<ApprovalPageResponse> getApprovedUsersByEnabledStatus(
      @Parameter(description = "Filter by enabled status (true for active, false for disabled)")
          @RequestParam
          Boolean enabled,
      @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20")
          int limit) {

    // Validate pagination parameters
    if (page < 1) page = 1;
    if (limit < 1) limit = 20;
    if (limit > 100) limit = 100;

    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

    Page<Sales> users = approvalService.getApprovedUsersByEnabledStatus(enabled, pageable);

    List<UserApprovalDto> userDtos =
        users.getContent().stream().map(this::toApprovalDto).collect(Collectors.toList());

    ApprovalPageResponse response =
        new ApprovalPageResponse(
            userDtos, users.getTotalElements(), page, limit, users.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Approve user registration",
      description = "Approve a pending user registration")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User approved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User already approved or invalid state")
      })
  @PostMapping("/{phone}/approve")
  public ResponseEntity<UserApprovalDto> approveUser(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone,
      @Parameter(description = "Approval action details") @Valid @RequestBody
          ApprovalActionRequest request) {

    String adminPhone = getCurrentUserPhone();
    SalesRole requestedRole = null;
    if (request.getSalesRole() != null && !request.getSalesRole().trim().isEmpty()) {
      try {
        requestedRole = SalesRole.valueOf(request.getSalesRole().toUpperCase());
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
      }
    }
    Sales approvedUser =
        approvalService.approveUser(phone, adminPhone, request.getReason(), requestedRole);

    return ResponseEntity.ok(toApprovalDto(approvedUser));
  }

  @Operation(
      summary = "Reject user registration",
      description = "Reject a pending user registration")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User rejected successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PostMapping("/{phone}/reject")
  public ResponseEntity<UserApprovalDto> rejectUser(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone,
      @Parameter(description = "Rejection action details") @Valid @RequestBody
          ApprovalActionRequest request) {

    String adminPhone = getCurrentUserPhone();
    Sales rejectedUser = approvalService.rejectUser(phone, adminPhone, request.getReason());

    return ResponseEntity.ok(toApprovalDto(rejectedUser));
  }

  @Operation(
      summary = "Reset user approval status",
      description = "Reset user approval status back to pending")
  @PostMapping("/{phone}/reset")
  public ResponseEntity<UserApprovalDto> resetUser(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone,
      @Parameter(description = "Reset action details") @Valid @RequestBody
          ApprovalActionRequest request) {

    String adminPhone = getCurrentUserPhone();
    Sales resetUser = approvalService.resetUserStatus(phone, adminPhone, request.getReason());

    return ResponseEntity.ok(toApprovalDto(resetUser));
  }

  @Operation(summary = "Enable user account", description = "Enable a disabled user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User enabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User not approved or already enabled")
      })
  @PostMapping("/{phone}/enable")
  public ResponseEntity<UserApprovalDto> enableUser(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone,
      @Parameter(description = "Enable action details") @Valid @RequestBody
          ApprovalActionRequest request) {

    String adminPhone = getCurrentUserPhone();
    Sales enabledUser = approvalService.enableUser(phone, adminPhone, request.getReason());

    return ResponseEntity.ok(toApprovalDto(enabledUser));
  }

  @Operation(summary = "Disable user account", description = "Disable an enabled user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User disabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User not approved or already disabled")
      })
  @PostMapping("/{phone}/disable")
  public ResponseEntity<UserApprovalDto> disableUser(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone,
      @Parameter(description = "Disable action details") @Valid @RequestBody
          ApprovalActionRequest request) {

    String adminPhone = getCurrentUserPhone();
    Sales disabledUser = approvalService.disableUser(phone, adminPhone, request.getReason());

    return ResponseEntity.ok(toApprovalDto(disabledUser));
  }

  @Operation(
      summary = "Reset user password",
      description = "Reset password for officer and customer agent users (admin only)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
      })
  @PostMapping("/{phone}/reset-password")
  public ResponseEntity<PasswordResetResponse> resetUserPassword(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone) {

    String temporaryPassword = approvalService.resetUserPassword(phone);
    return ResponseEntity.ok(new PasswordResetResponse(temporaryPassword, phone));
  }

  @Operation(
      summary = "Bulk approve or reject users",
      description = "Perform bulk approval operations")
  @PostMapping("/bulk-action")
  public ResponseEntity<BulkActionResponse> bulkAction(
      @Parameter(description = "Bulk action details") @Valid @RequestBody
          BulkApprovalRequest request) {

    String adminPhone = getCurrentUserPhone();
    int successCount = 0;

    if ("APPROVE".equals(request.getAction())) {
      successCount =
          approvalService.bulkApprove(request.getPhones(), adminPhone, request.getReason());
    } else if ("REJECT".equals(request.getAction())) {
      successCount =
          approvalService.bulkReject(request.getPhones(), adminPhone, request.getReason());
    } else {
      return ResponseEntity.badRequest()
          .body(new BulkActionResponse(0, request.getPhones().size(), "Invalid action"));
    }

    return ResponseEntity.ok(
        new BulkActionResponse(successCount, request.getPhones().size(), "Bulk action completed"));
  }

  @Operation(
      summary = "Bulk enable or disable users",
      description = "Perform bulk enable/disable operations")
  @PostMapping("/bulk-enable-disable")
  public ResponseEntity<BulkActionResponse> bulkEnableDisable(
      @Parameter(description = "Bulk enable/disable action details") @Valid @RequestBody
          BulkEnableDisableRequest request) {

    String adminPhone = getCurrentUserPhone();
    int successCount = 0;

    if ("ENABLE".equals(request.getAction())) {
      successCount =
          approvalService.bulkEnable(request.getPhones(), adminPhone, request.getReason());
    } else if ("DISABLE".equals(request.getAction())) {
      successCount =
          approvalService.bulkDisable(request.getPhones(), adminPhone, request.getReason());
    } else {
      return ResponseEntity.badRequest()
          .body(new BulkActionResponse(0, request.getPhones().size(), "Invalid action"));
    }

    return ResponseEntity.ok(
        new BulkActionResponse(successCount, request.getPhones().size(), "Bulk action completed"));
  }

  @Operation(
      summary = "Get user approval history",
      description = "Get approval history for a specific user")
  @GetMapping("/{phone}/history")
  public ResponseEntity<List<UserApprovalHistory>> getUserHistory(
      @Parameter(description = "User phone number", required = true) @PathVariable String phone) {

    List<UserApprovalHistory> history = approvalService.getUserApprovalHistory(phone);
    return ResponseEntity.ok(history);
  }

  @Operation(summary = "Get approval statistics", description = "Get overall approval statistics")
  @GetMapping("/statistics")
  public ResponseEntity<UserApprovalService.ApprovalStatistics> getStatistics() {
    UserApprovalService.ApprovalStatistics stats = approvalService.getApprovalStatistics();
    return ResponseEntity.ok(stats);
  }

  @Operation(summary = "Get recent approval activity", description = "Get recent approval actions")
  @GetMapping("/activity")
  public ResponseEntity<List<UserApprovalHistory>> getRecentActivity(
      @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "7")
          int days) {

    List<UserApprovalHistory> activity = approvalService.getRecentActivity(days);
    return ResponseEntity.ok(activity);
  }

  // Exception handlers
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
  }

  // Helper methods
  private String getCurrentUserPhone() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Sales) {
      Sales currentUser = (Sales) authentication.getPrincipal();
      if (currentUser.getRole() != SalesRole.ADMIN) {
        throw new IllegalStateException("Access denied - Admin role required");
      }
      return currentUser.getPhone();
    }
    throw new IllegalStateException("No authenticated admin user found");
  }

  private UserApprovalDto toApprovalDto(Sales user) {
    long daysWaiting = ChronoUnit.DAYS.between(user.getCreatedAt(), ZonedDateTime.now());
    return new UserApprovalDto(
        user.getPhone(),
        user.getName(),
        user.getRole() != null ? user.getRole().name() : null,
        user.getApprovalStatus(),
        user.getCreatedAt(),
        user.getStatusUpdatedAt(),
        daysWaiting,
        user.getApprovedByPhone(),
        user.getApprovedAt(),
        user.getRejectionReason(),
        user.getIsEnabled(),
        user.getDisabledAt(),
        user.getDisabledByPhone(),
        user.getDisabledReason());
  }

  // Request/Response DTOs

  /** Request DTO for approval actions. */
  public static class ApprovalActionRequest {
    private String reason;
    private String salesRole; // Optional: Allow admin to modify sales role during approval

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }

    public String getSalesRole() {
      return salesRole;
    }

    public void setSalesRole(String salesRole) {
      this.salesRole = salesRole;
    }
  }

  /** Request DTO for bulk approval operations. */
  public static class BulkApprovalRequest {
    private String action; // "APPROVE" or "REJECT"
    private List<String> phones;
    private String reason;

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public List<String> getPhones() {
      return phones;
    }

    public void setPhones(List<String> phones) {
      this.phones = phones;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  /** Request DTO for bulk enable/disable operations. */
  public static class BulkEnableDisableRequest {
    private String action; // "ENABLE" or "DISABLE"
    private List<String> phones;
    private String reason;

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public List<String> getPhones() {
      return phones;
    }

    public void setPhones(List<String> phones) {
      this.phones = phones;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  /** Response DTO for user approval information. */
  public static class UserApprovalDto {
    private String phone;
    private String name;
    private String role;
    private ApprovalStatus approvalStatus;
    private ZonedDateTime createdAt;
    private ZonedDateTime statusUpdatedAt;
    private long daysWaiting;
    private String approvedByPhone;
    private ZonedDateTime approvedAt;
    private String rejectionReason;
    private Boolean isEnabled;
    private ZonedDateTime disabledAt;
    private String disabledByPhone;
    private String disabledReason;

    public UserApprovalDto(
        String phone,
        String name,
        String role,
        ApprovalStatus approvalStatus,
        ZonedDateTime createdAt,
        ZonedDateTime statusUpdatedAt,
        long daysWaiting,
        String approvedByPhone,
        ZonedDateTime approvedAt,
        String rejectionReason,
        Boolean isEnabled,
        ZonedDateTime disabledAt,
        String disabledByPhone,
        String disabledReason) {
      this.phone = phone;
      this.name = name;
      this.role = role;
      this.approvalStatus = approvalStatus;
      this.createdAt = createdAt;
      this.statusUpdatedAt = statusUpdatedAt;
      this.daysWaiting = daysWaiting;
      this.approvedByPhone = approvedByPhone;
      this.approvedAt = approvedAt;
      this.rejectionReason = rejectionReason;
      this.isEnabled = isEnabled;
      this.disabledAt = disabledAt;
      this.disabledByPhone = disabledByPhone;
      this.disabledReason = disabledReason;
    }

    // Getters
    public String getPhone() {
      return phone;
    }

    public String getName() {
      return name;
    }

    public String getRole() {
      return role;
    }

    public ApprovalStatus getApprovalStatus() {
      return approvalStatus;
    }

    public ZonedDateTime getCreatedAt() {
      return createdAt;
    }

    public ZonedDateTime getStatusUpdatedAt() {
      return statusUpdatedAt;
    }

    public long getDaysWaiting() {
      return daysWaiting;
    }

    public String getApprovedByPhone() {
      return approvedByPhone;
    }

    public ZonedDateTime getApprovedAt() {
      return approvedAt;
    }

    public String getRejectionReason() {
      return rejectionReason;
    }

    public Boolean getIsEnabled() {
      return isEnabled;
    }

    public ZonedDateTime getDisabledAt() {
      return disabledAt;
    }

    public String getDisabledByPhone() {
      return disabledByPhone;
    }

    public String getDisabledReason() {
      return disabledReason;
    }
  }

  /** Response DTO for paginated approval lists. */
  public static class ApprovalPageResponse {
    private List<UserApprovalDto> items;
    private long total;
    private int page;
    private int limit;
    private int totalPages;

    public ApprovalPageResponse(
        List<UserApprovalDto> items, long total, int page, int limit, int totalPages) {
      this.items = items;
      this.total = total;
      this.page = page;
      this.limit = limit;
      this.totalPages = totalPages;
    }

    // Getters
    public List<UserApprovalDto> getItems() {
      return items;
    }

    public long getTotal() {
      return total;
    }

    public int getPage() {
      return page;
    }

    public int getLimit() {
      return limit;
    }

    public int getTotalPages() {
      return totalPages;
    }
  }

  /** Response DTO for bulk action results. */
  public static class BulkActionResponse {
    private int successCount;
    private int totalCount;
    private String message;

    public BulkActionResponse(int successCount, int totalCount, String message) {
      this.successCount = successCount;
      this.totalCount = totalCount;
      this.message = message;
    }

    // Getters
    public int getSuccessCount() {
      return successCount;
    }

    public int getTotalCount() {
      return totalCount;
    }

    public String getMessage() {
      return message;
    }
  }

  /** Response DTO for password reset operations. */
  public static class PasswordResetResponse {
    private String temporaryPassword;
    private String userPhone;

    public PasswordResetResponse(String temporaryPassword, String userPhone) {
      this.temporaryPassword = temporaryPassword;
      this.userPhone = userPhone;
    }

    public String getTemporaryPassword() {
      return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
      this.temporaryPassword = temporaryPassword;
    }

    public String getUserPhone() {
      return userPhone;
    }

    public void setUserPhone(String userPhone) {
      this.userPhone = userPhone;
    }
  }

  /** Response DTO for error messages. */
  public static class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }
}
