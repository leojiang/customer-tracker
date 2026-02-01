package com.example.customers.service;

import com.example.customers.model.Sales;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token operations.
 *
 * <p>Handles JWT token generation, validation, and claims extraction for authentication.
 */
@Service
public class JwtService {

  @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
  private String secretKey;

  @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
  private long jwtExpiration;

  public String extractPhone(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Long extractTokenVersion(String token) {
    return extractClaim(token, claims -> {
      Object versionObj = claims.get("tokenVersion");
      if (versionObj == null) {
        return 0L; // Default version for old tokens
      }
      if (versionObj instanceof Integer) {
        return ((Integer) versionObj).longValue();
      }
      if (versionObj instanceof Long) {
        return (Long) versionObj;
      }
      return 0L;
    });
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Generates JWT token for a sales user.
   *
   * @param sales the sales user to generate token for
   * @return JWT token string
   */
  public String generateToken(Sales sales) {
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("role", sales.getRole().name());
    extraClaims.put("id", sales.getId().toString());
    extraClaims.put("tokenVersion", sales.getTokenVersion() != null ? sales.getTokenVersion() : 0L);
    return generateToken(extraClaims, sales.getPhone());
  }

  /**
   * Generates JWT token with custom claims.
   *
   * @param extraClaims additional claims to include
   * @param phone user phone number as subject
   * @return JWT token string
   */
  public String generateToken(Map<String, Object> extraClaims, String phone) {
    return Jwts.builder()
        .setClaims(extraClaims)
        .setSubject(phone)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isTokenValid(String token, String phone) {
    final String extractedPhone = extractPhone(token);
    return (extractedPhone.equals(phone)) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().setSigningKey(getSignInKey()).parseClaimsJws(token).getBody();
  }

  private Key getSignInKey() {
    byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
  }
}
