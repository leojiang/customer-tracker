package com.example.customers.controller;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.service.AuthService;
import com.example.customers.service.AuthService.AuthResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication and authorization operations.
 *
 * <p>Handles user login, registration, and token validation for the application.
 */
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

  private final AuthService authService;

  @Autowired
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Authenticates user with phone and password.
   *
   * @param request login request containing phone and password
   * @return ResponseEntity containing auth response with token or error
   */
  @Operation(summary = "Login with phone and password")
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResult result = authService.login(request.getPhone(), request.getPassword());

    if (!result.isSuccess()) {
      HttpStatus status =
          "PENDING".equals(result.getStatus()) || "REJECTED".equals(result.getStatus())
              ? HttpStatus.FORBIDDEN
              : HttpStatus.UNAUTHORIZED;
      return ResponseEntity.status(status)
          .body(new AuthResponse(null, null, null, result.getMessage(), result.getStatus()));
    }

    return ResponseEntity.ok(
        new AuthResponse(
            result.getToken(),
            result.getPhone(),
            SalesRole.valueOf(result.getRole()),
            null,
            result.getStatus()));
  }

  /**
   * Registers a new sales user.
   *
   * @param request registration request containing phone, password, and role
   * @return ResponseEntity containing auth response with token or error
   */
  @Operation(summary = "Register new sales user")
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      return ResponseEntity.badRequest()
          .body(new AuthResponse(null, null, null, "register.passwordsDontMatch", null));
    }

    // Validate role selection - users cannot self-register as ADMIN
    if (request.getRole() == SalesRole.ADMIN) {
      return ResponseEntity.badRequest()
          .body(new AuthResponse(null, null, null, "register.cannotSelfRegisterAsAdmin", null));
    }

    try {
      AuthResult result =
          authService.register(request.getPhone(), request.getPassword(), request.getRole());

      if (result.isSuccess()) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                new AuthResponse(
                    null,
                    result.getPhone(),
                    request.getRole(),
                    result.getMessage(),
                    result.getStatus()));
      }

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponse(null, null, null, "auth.registerFailed", null));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(new AuthResponse(null, null, null, e.getMessage(), null));
    }
  }

  /**
   * Validates JWT token and returns user information.
   *
   * @param request token validation request
   * @return ResponseEntity containing user info or error
   */
  @Operation(summary = "Validate JWT token")
  @PostMapping("/validate")
  public ResponseEntity<AuthResponse> validateToken(
      @Valid @RequestBody ValidateTokenRequest request) {
    Optional<Sales> sales = authService.validateToken(request.getToken());

    if (sales.isPresent()) {
      return ResponseEntity.ok(
          new AuthResponse(
              request.getToken(), sales.get().getPhone(), sales.get().getRole(), null, null));
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new AuthResponse(null, null, null, "error.invalidToken", null));
  }

  /**
   * Changes password for authenticated user.
   *
   * @param request change password request
   * @return ResponseEntity containing success/error message
   */
  @Operation(summary = "Change user password")
  @PostMapping("/change-password")
  public ResponseEntity<AuthResponse> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    AuthResult result =
        authService.changePassword(
            request.getPhone(), request.getCurrentPassword(), request.getNewPassword());

    if (result.isSuccess()) {
      return ResponseEntity.ok(
          new AuthResponse(null, request.getPhone(), null, result.getMessage(), null));
    }

    return ResponseEntity.badRequest()
        .body(new AuthResponse(null, null, null, result.getMessage(), null));
  }

  // Request DTOs
  /** Login request DTO. */
  public static class LoginRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  /** Registration request DTO. */
  public static class RegisterRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    private SalesRole role = SalesRole.CUSTOMER_AGENT; // Default to CUSTOMER_AGENT

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getConfirmPassword() {
      return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
      this.confirmPassword = confirmPassword;
    }

    public SalesRole getRole() {
      return role;
    }

    public void setRole(SalesRole role) {
      this.role = role;
    }
  }

  /** Token validation request DTO. */
  public static class ValidateTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }
  }

  /** Change password request DTO. */
  public static class ChangePasswordRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getCurrentPassword() {
      return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
      this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
      return newPassword;
    }

    public void setNewPassword(String newPassword) {
      this.newPassword = newPassword;
    }
  }

  // Response DTO
  /** Authentication response DTO. */
  public static class AuthResponse {
    private String token;
    private String phone;
    private SalesRole role;
    private String error;
    private String status;

    /**
     * Constructor for AuthResponse.
     *
     * @param token JWT token
     * @param phone user phone number
     * @param role user role
     * @param error error message if any
     * @param status approval status
     */
    public AuthResponse(String token, String phone, SalesRole role, String error, String status) {
      this.token = token;
      this.phone = phone;
      this.role = role;
      this.error = error;
      this.status = status;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public SalesRole getRole() {
      return role;
    }

    public void setRole(SalesRole role) {
      this.role = role;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }
  }
}
