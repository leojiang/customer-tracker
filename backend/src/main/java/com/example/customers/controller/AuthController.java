package com.example.customers.controller;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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

  @Operation(summary = "Login with phone and password")
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Optional<String> tokenOptional = authService.login(request.getPhone(), request.getPassword());
    
    if (tokenOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new AuthResponse(null, null, null, "Invalid phone or password"));
    }
    
    String token = tokenOptional.get();
    Optional<Sales> sales = authService.getSalesByPhone(request.getPhone());
    
    if (sales.isPresent()) {
      return ResponseEntity.ok(new AuthResponse(
          token,
          sales.get().getPhone(),
          sales.get().getRole(),
          null
      ));
    }
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new AuthResponse(null, null, null, "Login successful but user data not found"));
  }

  @Operation(summary = "Register new sales user")
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      return ResponseEntity.badRequest()
          .body(new AuthResponse(null, null, null, "Passwords do not match"));
    }
    
    try {
      Optional<String> tokenOptional = authService.register(request.getPhone(), request.getPassword());
      
      if (tokenOptional.isPresent()) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponse(
                tokenOptional.get(),
                request.getPhone(),
                SalesRole.SALES,
                null
            ));
      }
      
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new AuthResponse(null, null, null, "Registration failed"));
      
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(new AuthResponse(null, null, null, e.getMessage()));
    }
  }

  @Operation(summary = "Validate JWT token")
  @PostMapping("/validate")
  public ResponseEntity<AuthResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
    Optional<Sales> sales = authService.validateToken(request.getToken());
    
    if (sales.isPresent()) {
      return ResponseEntity.ok(new AuthResponse(
          request.getToken(),
          sales.get().getPhone(),
          sales.get().getRole(),
          null
      ));
    }
    
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new AuthResponse(null, null, null, "Invalid or expired token"));
  }

  // Request DTOs
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

  public static class RegisterRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

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
  }

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

  // Response DTO
  public static class AuthResponse {
    private String token;
    private String phone;
    private SalesRole role;
    private String error;

    public AuthResponse(String token, String phone, SalesRole role, String error) {
      this.token = token;
      this.phone = phone;
      this.role = role;
      this.error = error;
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
  }
}