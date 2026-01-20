package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

  @InjectMocks private JwtService jwtService;

  private Sales testSales;
  private String testPhone;
  private UUID testId;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    testPhone = "+1234567890";

    testSales = new Sales();
    testSales.setId(testId);
    testSales.setPhone(testPhone);
    testSales.setRole(SalesRole.OFFICER);

    // Set test values using reflection
    ReflectionTestUtils.setField(
        jwtService,
        "secretKey",
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
  }

  @Test
  @DisplayName("Should generate token for sales user")
  void shouldGenerateTokenForSalesUser() {
    // When
    String token = jwtService.generateToken(testSales);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());

    // Verify token can be parsed and contains correct claims
    String extractedPhone = jwtService.extractPhone(token);
    assertEquals(testPhone, extractedPhone);

    // Verify custom claims
    Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
    assertEquals(testPhone, claims.getSubject());
    assertEquals(SalesRole.OFFICER.name(), claims.get("role"));
    assertEquals(testId.toString(), claims.get("id"));
  }

  @Test
  @DisplayName("Should generate token with custom claims")
  void shouldGenerateTokenWithCustomClaims() {
    // Given
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("role", "ADMIN");
    extraClaims.put("department", "IT");

    // When
    String token = jwtService.generateToken(extraClaims, testPhone);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());

    // Verify token contains correct claims
    String extractedPhone = jwtService.extractPhone(token);
    assertEquals(testPhone, extractedPhone);

    Claims claims = jwtService.extractClaim(token, claims1 -> claims1);
    assertEquals(testPhone, claims.getSubject());
    assertEquals("ADMIN", claims.get("role"));
    assertEquals("IT", claims.get("department"));
  }

  @Test
  @DisplayName("Should extract phone from token")
  void shouldExtractPhoneFromToken() {
    // Given
    String token = jwtService.generateToken(testSales);

    // When
    String extractedPhone = jwtService.extractPhone(token);

    // Then
    assertEquals(testPhone, extractedPhone);
  }

  @Test
  @DisplayName("Should extract custom claim from token")
  void shouldExtractCustomClaimFromToken() {
    // Given
    String token = jwtService.generateToken(testSales);

    // When
    String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

    // Then
    assertEquals(SalesRole.OFFICER.name(), role);
  }

  @Test
  @DisplayName("Should validate token successfully")
  void shouldValidateTokenSuccessfully() {
    // Given
    String token = jwtService.generateToken(testSales);

    // When
    boolean isValid = jwtService.isTokenValid(token, testPhone);

    // Then
    assertTrue(isValid);
  }

  @Test
  @DisplayName("Should invalidate token with wrong phone")
  void shouldInvalidateTokenWithWrongPhone() {
    // Given
    String token = jwtService.generateToken(testSales);
    String wrongPhone = "+9876543210";

    // When
    boolean isValid = jwtService.isTokenValid(token, wrongPhone);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("Should invalidate malformed token")
  void shouldInvalidateMalformedToken() {
    // Given
    String malformedToken = "invalid.token.here";

    // When & Then
    assertThrows(MalformedJwtException.class, () -> jwtService.extractPhone(malformedToken));
    assertThrows(
        MalformedJwtException.class, () -> jwtService.isTokenValid(malformedToken, testPhone));
  }

  @Test
  @DisplayName("Should invalidate token with wrong signature")
  void shouldInvalidateTokenWithWrongSignature() {
    // Given
    String tokenWithWrongSignature =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIrMTIzNDU2Nzg5MCIsImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDg2NDAwfQ.wrongsignature";

    // When & Then
    assertThrows(SignatureException.class, () -> jwtService.extractPhone(tokenWithWrongSignature));
    assertThrows(
        SignatureException.class,
        () -> jwtService.isTokenValid(tokenWithWrongSignature, testPhone));
  }

  @Test
  @DisplayName("Should invalidate expired token")
  void shouldInvalidateExpiredToken() {
    // Given - Create a token with very short expiration (1ms ago)
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L); // Expired
    String expiredToken = jwtService.generateToken(testSales);

    // When & Then
    assertThrows(ExpiredJwtException.class, () -> jwtService.extractPhone(expiredToken));
    assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(expiredToken, testPhone));
  }

  @Test
  @DisplayName("Should handle unsupported JWT token")
  void shouldHandleUnsupportedJwtToken() {
    // Given
    String unsupportedToken = "unsupported.token.format";

    // When & Then
    assertThrows(Exception.class, () -> jwtService.extractPhone(unsupportedToken));
    assertThrows(Exception.class, () -> jwtService.isTokenValid(unsupportedToken, testPhone));
  }

  @Test
  @DisplayName("Should handle empty token")
  void shouldHandleEmptyToken() {
    // Given
    String emptyToken = "";

    // When & Then
    assertThrows(Exception.class, () -> jwtService.extractPhone(emptyToken));
    assertThrows(Exception.class, () -> jwtService.isTokenValid(emptyToken, testPhone));
  }

  @Test
  @DisplayName("Should handle null token")
  void shouldHandleNullToken() {
    // Given
    String nullToken = null;

    // When & Then
    assertThrows(Exception.class, () -> jwtService.extractPhone(nullToken));
    assertThrows(Exception.class, () -> jwtService.isTokenValid(nullToken, testPhone));
  }

  @Test
  @DisplayName("Should generate different tokens for different users")
  void shouldGenerateDifferentTokensForDifferentUsers() {
    // Given
    Sales anotherSales = new Sales();
    anotherSales.setId(UUID.randomUUID());
    anotherSales.setPhone("+9876543210");
    anotherSales.setRole(SalesRole.ADMIN);

    // When
    String token1 = jwtService.generateToken(testSales);
    String token2 = jwtService.generateToken(anotherSales);

    // Then
    assertNotEquals(token1, token2);

    // Verify both tokens are valid for their respective users
    assertTrue(jwtService.isTokenValid(token1, testPhone));
    assertTrue(jwtService.isTokenValid(token2, "+9876543210"));

    // Verify tokens are not valid for each other's users
    assertFalse(jwtService.isTokenValid(token1, "+9876543210"));
    assertFalse(jwtService.isTokenValid(token2, testPhone));
  }

  @Test
  @DisplayName("Should generate tokens with correct expiration")
  void shouldGenerateTokensWithCorrectExpiration() {
    // Given
    long expectedExpiration = System.currentTimeMillis() + 86400000L; // 24 hours
    String token = jwtService.generateToken(testSales);

    // When
    Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

    // Then
    assertNotNull(expiration);
    // Allow for small time differences (within 1 second)
    assertTrue(Math.abs(expiration.getTime() - expectedExpiration) < 1000);
  }

  @Test
  @DisplayName("Should generate tokens with correct issued at time")
  void shouldGenerateTokensWithCorrectIssuedAtTime() {
    // Given
    long beforeGeneration = System.currentTimeMillis();
    String token = jwtService.generateToken(testSales);
    long afterGeneration = System.currentTimeMillis();

    // When
    Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

    // Then
    assertNotNull(issuedAt);
    // Allow for small time differences (within 1 second)
    assertTrue(issuedAt.getTime() >= beforeGeneration - 1000);
    assertTrue(issuedAt.getTime() <= afterGeneration + 1000);
  }

  @Test
  @DisplayName("Should handle token with no custom claims")
  void shouldHandleTokenWithNoCustomClaims() {
    // Given
    Map<String, Object> emptyClaims = new HashMap<>();
    String token = jwtService.generateToken(emptyClaims, testPhone);

    // When
    String extractedPhone = jwtService.extractPhone(token);
    boolean isValid = jwtService.isTokenValid(token, testPhone);

    // Then
    assertEquals(testPhone, extractedPhone);
    assertTrue(isValid);
  }

  @Test
  @DisplayName("Should handle token with multiple custom claims")
  void shouldHandleTokenWithMultipleCustomClaims() {
    // Given
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", "SALES");
    claims.put("department", "Sales");
    claims.put("region", "North");
    claims.put("level", "Senior");

    String token = jwtService.generateToken(claims, testPhone);

    // When
    Claims extractedClaims = jwtService.extractClaim(token, claims1 -> claims1);

    // Then
    assertEquals(testPhone, extractedClaims.getSubject());
    assertEquals("SALES", extractedClaims.get("role"));
    assertEquals("Sales", extractedClaims.get("department"));
    assertEquals("North", extractedClaims.get("region"));
    assertEquals("Senior", extractedClaims.get("level"));
  }
}
