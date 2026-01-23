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

  // Change Password Tests

  @Test
  @DisplayName("Should change password successfully with valid data")
  void shouldChangePasswordSuccessfullyWithValidData() {
    // Given
    String currentPassword = "currentPassword";
    String newPassword = "newPassword123";
    String newHashedPassword = "$2a$10$newhashedpassword";

    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(currentPassword, testHashedPassword)).thenReturn(true);
    when(passwordEncoder.matches(newPassword, testHashedPassword)).thenReturn(false);
    when(passwordEncoder.encode(newPassword)).thenReturn(newHashedPassword);
    when(salesRepository.save(any(Sales.class))).thenReturn(testSales);

    // When
    AuthService.AuthResult result =
        authService.changePassword(testPhone, currentPassword, newPassword);

    // Then
    assertTrue(result.isSuccess());
    assertEquals("password.success.changed", result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(currentPassword, testHashedPassword);
    verify(passwordEncoder).matches(newPassword, testHashedPassword);
    verify(passwordEncoder).encode(newPassword);
    verify(salesRepository).save(argThat(sales -> sales.getPassword().equals(newHashedPassword)));
  }

  @Test
  @DisplayName("Should fail when phone is null or empty")
  void shouldFailWhenPhoneIsNullOrEmpty() {
    // When
    AuthService.AuthResult result1 = authService.changePassword(null, "currentPass", "newPass123");
    AuthService.AuthResult result2 = authService.changePassword("", "currentPass", "newPass123");
    AuthService.AuthResult result3 = authService.changePassword("   ", "currentPass", "newPass123");

    // Then
    assertFalse(result1.isSuccess());
    assertEquals("error.phoneRequired", result1.getMessage());

    assertFalse(result2.isSuccess());
    assertEquals("error.phoneRequired", result2.getMessage());

    assertFalse(result3.isSuccess());
    assertEquals("error.phoneRequired", result3.getMessage());

    verify(salesRepository, never()).findByPhone(any());
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  @DisplayName("Should fail when current password is null or empty")
  void shouldFailWhenCurrentPasswordIsNullOrEmpty() {
    // When
    AuthService.AuthResult result1 = authService.changePassword(testPhone, null, "newPass123");
    AuthService.AuthResult result2 = authService.changePassword(testPhone, "", "newPass123");
    AuthService.AuthResult result3 = authService.changePassword(testPhone, "   ", "newPass123");

    // Then
    assertFalse(result1.isSuccess());
    assertEquals("error.currentPasswordRequired", result1.getMessage());

    assertFalse(result2.isSuccess());
    assertEquals("error.currentPasswordRequired", result2.getMessage());

    assertFalse(result3.isSuccess());
    assertEquals("error.currentPasswordRequired", result3.getMessage());

    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  @DisplayName("Should fail when new password is null or empty")
  void shouldFailWhenNewPasswordIsNullOrEmpty() {
    // When
    AuthService.AuthResult result1 = authService.changePassword(testPhone, "currentPass", null);
    AuthService.AuthResult result2 = authService.changePassword(testPhone, "currentPass", "");
    AuthService.AuthResult result3 = authService.changePassword(testPhone, "currentPass", "   ");

    // Then
    assertFalse(result1.isSuccess());
    assertEquals("error.newPasswordRequired", result1.getMessage());

    assertFalse(result2.isSuccess());
    assertEquals("error.newPasswordRequired", result2.getMessage());

    assertFalse(result3.isSuccess());
    assertEquals("error.newPasswordRequired", result3.getMessage());

    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Should fail when user not found")
  void shouldFailWhenUserNotFound() {
    // Given
    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.empty());

    // When
    AuthService.AuthResult result =
        authService.changePassword(testPhone, "currentPass", "newPass123");

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.userNotFound", result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  @DisplayName("Should fail when current password is incorrect")
  void shouldFailWhenCurrentPasswordIsIncorrect() {
    // Given
    String currentPassword = "wrongPassword";
    String newPassword = "newPassword123";

    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(currentPassword, testHashedPassword)).thenReturn(false);

    // When
    AuthService.AuthResult result =
        authService.changePassword(testPhone, currentPassword, newPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.incorrectCurrentPassword", result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(currentPassword, testHashedPassword);
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Should fail when new password is too short")
  void shouldFailWhenNewPasswordIsTooShort() {
    // Given
    String currentPassword = "currentPassword";
    String newPassword = "12345"; // Less than 6 characters

    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    when(passwordEncoder.matches(currentPassword, testHashedPassword)).thenReturn(true);

    // When
    AuthService.AuthResult result =
        authService.changePassword(testPhone, currentPassword, newPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.passwordTooShort", result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder).matches(currentPassword, testHashedPassword);
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Should fail when new password is same as current password")
  void shouldFailWhenNewPasswordIsSameAsCurrentPassword() {
    // Given
    String currentPassword = "currentPassword";
    String newPassword = "currentPassword"; // Same as current

    when(salesRepository.findByPhone(testPhone)).thenReturn(Optional.of(testSales));
    // First call for current password verification (should match)
    when(passwordEncoder.matches(currentPassword, testHashedPassword)).thenReturn(true);
    // The service checks if new password is different from current password
    // Since both are "currentPassword", the second check should also return true
    when(passwordEncoder.matches(newPassword, testHashedPassword)).thenReturn(true);

    // When
    AuthService.AuthResult result =
        authService.changePassword(testPhone, currentPassword, newPassword);

    // Then
    assertFalse(result.isSuccess());
    assertEquals("error.newPasswordSameAsOld", result.getMessage());

    verify(salesRepository).findByPhone(testPhone);
    verify(passwordEncoder, times(2)).matches(anyString(), eq(testHashedPassword));
    verify(passwordEncoder, never()).encode(any());
    verify(salesRepository, never()).save(any());
  }
}
