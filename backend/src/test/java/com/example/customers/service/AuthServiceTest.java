package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.repository.SalesRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

  @Mock private SalesRepository salesRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  @InjectMocks private AuthService authService;

  private Sales testSales;
  private String testPhone;
  private String testPassword;
  private String testHashedPassword;
  private String testToken;

  @BeforeEach
  void setUp() {
    testPhone = "+1234567890";
    testPassword = "password123";
    testHashedPassword = "$2a$10$hashedpassword";
    testToken = "jwt.token.here";

    testSales = new Sales();
    testSales.setPhone(testPhone);
    testSales.setPassword(testHashedPassword);
    testSales.setRole(SalesRole.OFFICER);
    testSales.setApprovalStatus(ApprovalStatus.APPROVED);
  }

  @Test
  @DisplayName("Should login successfully with valid credentials")
  void shouldLoginSuccessfullyWithValidCredentials() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(testPassword, testHashedPassword)).thenReturn(true);
    when(jwtService.generateToken(testSales)).thenReturn(testToken);

    // When
    AuthService.AuthResult result = authService.login(testPhone, testPassword);

    // Then
    assertTrue(result.isSuccess());
    assertEquals(testToken, result.getToken());
    assertEquals(testPhone, result.getPhone());
    assertEquals("OFFICER", result.getRole());
    assertEquals("APPROVED", result.getStatus());
    assertNull(result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(testPassword, testHashedPassword);
    verify(jwtService).generateToken(testSales);
  }

  @Test
  @DisplayName("Should fail login when user not found")
  void shouldFailLoginWhenUserNotFound() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.empty());

    // When
    AuthService.AuthResult result = authService.login(testPhone, testPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.incorrectCredentials", result.getMessage());
    assertNull(result.getToken());
    assertNull(result.getPhone());
    assertNull(result.getRole());
    assertNull(result.getStatus());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder, never()).matches(any(), any());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  @DisplayName("Should fail login with incorrect password")
  void shouldFailLoginWithIncorrectPassword() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(testPassword, testHashedPassword)).thenReturn(false);

    // When
    AuthService.AuthResult result = authService.login(testPhone, testPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.incorrectCredentials", result.getMessage());
    assertNull(result.getToken());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(testPassword, testHashedPassword);
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  @DisplayName("Should fail login when account is pending")
  void shouldFailLoginWhenAccountIsPending() {
    // Given
    testSales.setApprovalStatus(ApprovalStatus.PENDING);
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(testPassword, testHashedPassword)).thenReturn(true);

    // When
    AuthService.AuthResult result = authService.login(testPhone, testPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.accountPending", result.getMessage());
    assertEquals("PENDING", result.getStatus());
    assertNull(result.getToken());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(testPassword, testHashedPassword);
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  @DisplayName("Should fail login when account is rejected")
  void shouldFailLoginWhenAccountIsRejected() {
    // Given
    testSales.setApprovalStatus(ApprovalStatus.REJECTED);
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(testPassword, testHashedPassword)).thenReturn(true);

    // When
    AuthService.AuthResult result = authService.login(testPhone, testPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.accountDenied", result.getMessage());
    assertEquals("REJECTED", result.getStatus());
    assertNull(result.getToken());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(testPassword, testHashedPassword);
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  @DisplayName("Should register new user successfully")
  void shouldRegisterNewUserSuccessfully() {
    // Given
    when(salesRepository.existsByPhone(testPhone)).thenReturn(false);
    when(passwordEncoder.encode(testPassword)).thenReturn(testHashedPassword);
    when(salesRepository.save(any(Sales.class))).thenReturn(testSales);

    // When
    AuthService.AuthResult result = authService.register(testPhone, testPassword);

    // Then
    assertTrue(result.isSuccess());
    assertEquals("register.success.message", result.getMessage());
    assertEquals(testPhone, result.getPhone());
    assertEquals("PENDING", result.getStatus());
    assertNull(result.getToken());
    assertNull(result.getRole());

    verify(salesRepository).existsByPhone(testPhone);
    verify(passwordEncoder).encode(testPassword);
    verify(salesRepository)
        .save(
            argThat(
                sales ->
                    sales.getPhone().equals(testPhone)
                        && sales.getPassword().equals(testHashedPassword)
                        && sales.getRole() == SalesRole.CUSTOMER_AGENT
                        && sales.getApprovalStatus() == ApprovalStatus.PENDING));
  }

  @Test
  @DisplayName("Should throw exception when registering with existing phone")
  void shouldThrowExceptionWhenRegisteringWithExistingPhone() {
    // Given
    when(salesRepository.existsByPhone(testPhone)).thenReturn(true);

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> authService.register(testPhone, testPassword));

    assertEquals("error.phoneAlreadyExists", exception.getMessage());

    verify(salesRepository).existsByPhone(testPhone);
    verify(passwordEncoder, never()).encode(any());
    verify(salesRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get sales by phone successfully")
  void shouldGetSalesByPhoneSuccessfully() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));

    // When
    Optional<Sales> result = authService.getSalesByPhone(testPhone);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSales, result.get());

    verify(salesRepository).findByPhone(testPhone);
  }

  @Test
  @DisplayName("Should return empty when sales not found by phone")
  void shouldReturnEmptyWhenSalesNotFoundByPhone() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.empty());

    // When
    Optional<Sales> result = authService.getSalesByPhone(testPhone);

    // Then
    assertFalse(result.isPresent());

    verify(salesRepository).findByPhone(testPhone);
  }

  @Test
  @DisplayName("Should validate token successfully")
  void shouldValidateTokenSuccessfully() {
    // Given
    when(jwtService.extractPhone(testToken)).thenReturn(testPhone);
    when(jwtService.isTokenValid(testToken, testPhone)).thenReturn(true);
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));

    // When
    Optional<Sales> result = authService.validateToken(testToken);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSales, result.get());

    verify(jwtService).extractPhone(testToken);
    verify(jwtService).isTokenValid(testToken, testPhone);
    verify(salesRepository).findByPhone(testPhone);
  }

  @Test
  @DisplayName("Should return empty when token is invalid")
  void shouldReturnEmptyWhenTokenIsInvalid() {
    // Given
    when(jwtService.extractPhone(testToken)).thenReturn(testPhone);
    when(jwtService.isTokenValid(testToken, testPhone)).thenReturn(false);

    // When
    Optional<Sales> result = authService.validateToken(testToken);

    // Then
    assertFalse(result.isPresent());

    verify(jwtService).extractPhone(testToken);
    verify(jwtService).isTokenValid(testToken, testPhone);
    verify(salesRepository, never()).findByPhone(any());
  }

  @Test
  @DisplayName("Should return empty when token extraction throws exception")
  void shouldReturnEmptyWhenTokenExtractionThrowsException() {
    // Given
    when(jwtService.extractPhone(testToken)).thenThrow(new RuntimeException("Invalid token"));

    // When
    Optional<Sales> result = authService.validateToken(testToken);

    // Then
    assertFalse(result.isPresent());

    verify(jwtService).extractPhone(testToken);
    verify(jwtService, never()).isTokenValid(any(), any());
    verify(salesRepository, never()).findByPhone(any());
  }

  @Test
  @DisplayName("Should return empty when user not found for valid token")
  void shouldReturnEmptyWhenUserNotFoundForValidToken() {
    // Given
    when(jwtService.extractPhone(testToken)).thenReturn(testPhone);
    when(jwtService.isTokenValid(testToken, testPhone)).thenReturn(true);
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.empty());

    // When
    Optional<Sales> result = authService.validateToken(testToken);

    // Then
    assertFalse(result.isPresent());

    verify(jwtService).extractPhone(testToken);
    verify(jwtService).isTokenValid(testToken, testPhone);
    verify(salesRepository).findByPhone(testPhone);
  }

  @Test
  @DisplayName("AuthResult success should have correct values")
  void authResultSuccessShouldHaveCorrectValues() {
    // When
    AuthService.AuthResult result = AuthService.AuthResult.success(testToken, testPhone, "SALES");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(testToken, result.getToken());
    assertEquals(testPhone, result.getPhone());
    assertEquals("SALES", result.getRole());
    assertEquals("APPROVED", result.getStatus());
    assertNull(result.getMessage());
  }

  @Test
  @DisplayName("AuthResult failure should have correct values")
  void authResultFailureShouldHaveCorrectValues() {
    // When
    AuthService.AuthResult result = AuthService.AuthResult.failure("error.message", "PENDING");

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.message", result.getMessage());
    assertEquals("PENDING", result.getStatus());
    assertNull(result.getToken());
    assertNull(result.getPhone());
    assertNull(result.getRole());
  }

  @Test
  @DisplayName("AuthResult registration success should have correct values")
  void authResultRegistrationSuccessShouldHaveCorrectValues() {
    // When
    AuthService.AuthResult result =
        AuthService.AuthResult.registrationSuccess("success.message", testPhone, "PENDING");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("success.message", result.getMessage());
    assertEquals(testPhone, result.getPhone());
    assertEquals("PENDING", result.getStatus());
    assertNull(result.getToken());
    assertNull(result.getRole());
  }
}
