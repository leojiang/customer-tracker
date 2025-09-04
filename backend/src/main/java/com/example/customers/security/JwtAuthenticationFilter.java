package com.example.customers.security;

import com.example.customers.model.Sales;
import com.example.customers.service.AuthService;
import com.example.customers.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AuthService authService;

  @Autowired
  public JwtAuthenticationFilter(JwtService jwtService, AuthService authService) {
    this.jwtService = jwtService;
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Skip JWT processing for Swagger endpoints and auth endpoints
    String requestPath = request.getRequestURI();
    if (requestPath.startsWith("/swagger-ui")
        || requestPath.startsWith("/v3/api-docs")
        || requestPath.startsWith("/api/v3/api-docs")
        || // Legacy path support
        requestPath.startsWith("/api/auth")
        || requestPath.startsWith("/api/health")
        || requestPath.startsWith("/swagger-resources")
        || requestPath.startsWith("/webjars")
        || requestPath.equals("/swagger-ui.html")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final String jwt = authHeader.substring(7);
      final String phone = jwtService.extractPhone(jwt);

      if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        Optional<Sales> salesOptional = authService.getSalesByPhone(phone);

        if (salesOptional.isPresent() && jwtService.isTokenValid(jwt, phone)) {
          Sales sales = salesOptional.get();

          List<SimpleGrantedAuthority> authorities =
              List.of(new SimpleGrantedAuthority("ROLE_" + sales.getRole().name()));

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(sales, null, authorities);

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      // Invalid token, continue with unauthenticated request
      logger.debug("JWT token validation failed", e);
    }

    filterChain.doFilter(request, response);
  }
}
