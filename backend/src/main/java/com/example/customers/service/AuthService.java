package com.example.customers.service;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.repository.SalesRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final SalesRepository salesRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Autowired
  public AuthService(
      SalesRepository salesRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.salesRepository = salesRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

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
