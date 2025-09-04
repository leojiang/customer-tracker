package com.example.customers.service;

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
   * @return Optional JWT token if authentication successful
   */
  public Optional<String> login(String phone, String password) {
    Optional<Sales> salesOptional = salesRepository.findByPhone(phone);

    if (salesOptional.isEmpty()) {
      return Optional.empty();
    }

    Sales sales = salesOptional.get();

    if (passwordEncoder.matches(password, sales.getPassword())) {
      String token = jwtService.generateToken(sales);
      return Optional.of(token);
    }

    return Optional.empty();
  }

  /**
   * Registers new sales user.
   *
   * @param phone user phone number
   * @param password user password
   * @return Optional JWT token if registration successful
   * @throws IllegalArgumentException if phone already exists
   */
  public Optional<String> register(String phone, String password) {
    if (salesRepository.existsByPhone(phone)) {
      throw new IllegalArgumentException("Phone number already exists");
    }

    String hashedPassword = passwordEncoder.encode(password);
    Sales sales = new Sales(phone, hashedPassword, SalesRole.SALES);
    Sales savedSales = salesRepository.save(sales);

    String token = jwtService.generateToken(savedSales);
    return Optional.of(token);
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
}
