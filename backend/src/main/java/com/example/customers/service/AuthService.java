package com.example.customers.service;

import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.repository.SalesRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for authentication operations.
 *
 * <p>Handles user login, registration, and token validation for sales users.
 */
@Service
public class AuthService {

  private final SalesRepository salesRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  /**
   * Constructor for AuthService.
   *
   * @param salesRepository repository for sales data
   * @param passwordEncoder password encoder for hashing
   * @param jwtService JWT token service
   */
  @Autowired
  public AuthService(
      SalesRepository salesRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.salesRepository = salesRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  /**
   * Authenticates user with phone and password.
   *
   * @param phone user phone number
   * @param password user password
   * @return authentication result with token or error status
   */
  public AuthResult login(String phone, String password) {
    Optional<Sales> salesOptional = salesRepository.findByPhone(phone);

    if (salesOptional.isEmpty()) {
      return AuthResult.failure("error.incorrectCredentials", null);
    }

    Sales sales = salesOptional.get();

    if (!passwordEncoder.matches(password, sales.getPassword())) {
      return AuthResult.failure("error.incorrectCredentials", null);
    }

    // Check approval status
    if (!sales.isApproved()) {
      if (sales.isPending()) {
        return AuthResult.failure("error.accountPending", "PENDING");
      } else if (sales.isRejected()) {
        return AuthResult.failure("error.accountDenied", "REJECTED");
      }
    }

    // Check if account is enabled
    if (sales.isDisabled()) {
      return AuthResult.failure("error.accountDisabled", "DISABLED");
    }

    String token = jwtService.generateToken(sales);
    return AuthResult.success(token, sales.getPhone(), sales.getRole().name());
  }

  /**
   * Registers new sales user.
   *
   * @param phone user phone number
   * @param password user password
   * @param role user role (OFFICER or CUSTOMER_AGENT)
   * @return registration result with status information
   * @throws IllegalArgumentException if phone already exists
   */
  public AuthResult register(String phone, String password, SalesRole role) {
    if (salesRepository.existsByPhone(phone)) {
      throw new IllegalArgumentException("error.phoneAlreadyExists");
    }

    String hashedPassword = passwordEncoder.encode(password);
    Sales sales = new Sales(phone, hashedPassword, role);
    // New users start with PENDING status (set in migration default)
    sales.setApprovalStatus(ApprovalStatus.PENDING);
    Sales savedSales = salesRepository.save(sales);

    return AuthResult.registrationSuccess(
        "register.success.message", savedSales.getPhone(), "PENDING");
  }

  /**
   * Register a new user with default role (CUSTOMER_AGENT).
   *
   * @param phone user phone number
   * @param password user password
   * @return registration result with status information
   * @throws IllegalArgumentException if phone already exists
   */
  public AuthResult register(String phone, String password) {
    return register(phone, password, SalesRole.CUSTOMER_AGENT);
  }

  public Optional<Sales> getSalesByPhone(String phone) {
    return salesRepository.findByPhone(phone);
  }

  /**
   * Validates JWT token and returns user.
   *
   * @param token JWT token to validate
   * @return Optional Sales user if token is valid
   */
  public Optional<Sales> validateToken(String token) {
    try {
      String phone = jwtService.extractPhone(token);
      if (jwtService.isTokenValid(token, phone)) {
        return salesRepository.findByPhone(phone);
      }
    } catch (Exception e) {
      // Token is invalid
    }
    return Optional.empty();
  }

  /**
   * Changes password for authenticated user.
   *
   * @param phone user phone number
   * @param currentPassword current password for verification
   * @param newPassword new password to set
   * @return result indicating success or failure with message
   */
  public AuthResult changePassword(String phone, String currentPassword, String newPassword) {
    // Validate input parameters
    if (phone == null || phone.trim().isEmpty()) {
      return AuthResult.failure("error.phoneRequired", null);
    }
    if (currentPassword == null || currentPassword.trim().isEmpty()) {
      return AuthResult.failure("error.currentPasswordRequired", null);
    }
    if (newPassword == null || newPassword.trim().isEmpty()) {
      return AuthResult.failure("error.newPasswordRequired", null);
    }

    // Find user
    Optional<Sales> salesOptional = salesRepository.findByPhone(phone);
    if (salesOptional.isEmpty()) {
      return AuthResult.failure("error.userNotFound", null);
    }

    Sales sales = salesOptional.get();

    // Verify current password
    if (!passwordEncoder.matches(currentPassword, sales.getPassword())) {
      return AuthResult.failure("error.incorrectCurrentPassword", null);
    }

    // Validate new password length
    if (newPassword.length() < 6) {
      return AuthResult.failure("error.passwordTooShort", null);
    }

    // Ensure new password is different from current password
    if (passwordEncoder.matches(newPassword, sales.getPassword())) {
      return AuthResult.failure("error.newPasswordSameAsOld", null);
    }

    // Hash and update new password
    String hashedNewPassword = passwordEncoder.encode(newPassword);
    sales.setPassword(hashedNewPassword);
    salesRepository.save(sales);

    return AuthResult.successWithMessage("password.success.changed", null, null);
  }

  /** Result class for authentication operations. */
  public static class AuthResult {
    private final boolean success;
    private final String message;
    private final String token;
    private final String phone;
    private final String role;
    private final String status;

    private AuthResult(
        boolean success, String message, String token, String phone, String role, String status) {
      this.success = success;
      this.message = message;
      this.token = token;
      this.phone = phone;
      this.role = role;
      this.status = status;
    }

    public static AuthResult success(String token, String phone, String role) {
      return new AuthResult(true, null, token, phone, role, "APPROVED");
    }

    public static AuthResult successWithMessage(String message, String token, String phone) {
      return new AuthResult(true, message, token, phone, null, null);
    }

    public static AuthResult failure(String message, String status) {
      return new AuthResult(false, message, null, null, null, status);
    }

    public static AuthResult registrationSuccess(String message, String phone, String status) {
      return new AuthResult(true, message, null, phone, null, status);
    }

    // Getters
    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public String getToken() {
      return token;
    }

    public String getPhone() {
      return phone;
    }

    public String getRole() {
      return role;
    }

    public String getStatus() {
      return status;
    }
  }
}
