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

@Service
public class JwtService {

  @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
  private String secretKey;

  @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
  private long jwtExpiration;

  public String extractPhone(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(Sales sales) {
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("role", sales.getRole().name());
    extraClaims.put("id", sales.getId().toString());
    return generateToken(extraClaims, sales.getPhone());
  }

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
