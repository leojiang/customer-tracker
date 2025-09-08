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
      return AuthResult.failure("Invalid credentials", null);
    }

    Sales sales = salesOptional.get();

    if (!passwordEncoder.matches(password, sales.getPassword())) {
      return AuthResult.failure("Invalid credentials", null);
    }

    // Check approval status
    if (!sales.isApproved()) {
      if (sales.isPending()) {
        return AuthResult.failure("Account pending approval. Please contact admin.", "PENDING");
      } else if (sales.isRejected()) {
        return AuthResult.failure("Account access denied. Contact admin for more information.", "REJECTED");
      }
    }

    String token = jwtService.generateToken(sales);
    return AuthResult.success(token, sales.getPhone(), sales.getRole().name());
  }

  /**
   * Registers new sales user.
   *
   * @param phone user phone number
   * @param password user password
   * @return registration result with status information
   * @throws IllegalArgumentException if phone already exists
   */
  public AuthResult register(String phone, String password) {
    if (salesRepository.existsByPhone(phone)) {
      throw new IllegalArgumentException("Phone number already exists");
    }

    String hashedPassword = passwordEncoder.encode(password);
    Sales sales = new Sales(phone, hashedPassword, SalesRole.SALES);
    // New users start with PENDING status (set in migration default)
    sales.setApprovalStatus(ApprovalStatus.PENDING);
    Sales savedSales = salesRepository.save(sales);

    return AuthResult.registrationSuccess(
        "Registration submitted successfully. Your account is pending admin approval.",
        savedSales.getPhone(),
        "PENDING");
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

  /** Result class for authentication operations. */
  public static class AuthResult {
    private final boolean success;
    private final String message;
    private final String token;
    private final String phone;
    private final String role;
    private final String status;

    private AuthResult(boolean success, String message, String token, String phone, String role, String status) {
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
