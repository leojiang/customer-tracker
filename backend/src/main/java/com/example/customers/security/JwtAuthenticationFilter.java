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

/**
 * JWT authentication filter for processing JWT tokens in requests.
 *
 * <p>Validates JWT tokens and sets up security context for authenticated users.
 */
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
        // Use validateToken which checks token version
        Optional<Sales> salesOptional = authService.validateToken(jwt);

        if (salesOptional.isPresent()) {
          Sales sales = salesOptional.get();

          List<SimpleGrantedAuthority> authorities =
              List.of(new SimpleGrantedAuthority(sales.getRole().name()));

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(sales, null, authorities);

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
          // Token is invalid (expired or session conflict)
          // Set response header to indicate session conflict
          Long tokenVersion = jwtService.extractTokenVersion(jwt);
          Optional<Sales> userCheck = authService.getSalesByPhone(phone);

          if (userCheck.isPresent() && !userCheck.get().getTokenVersion().equals(tokenVersion)) {
            // Session conflict: user logged in elsewhere
            response.setHeader("X-Session-Conflict", "true");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"error.sessionConflict\", \"message\": \"Session invalidated due to login from another device\"}");
            return;
          }
        }
      }
    } catch (Exception e) {
      // Invalid token, continue with unauthenticated request
      // Silent failure - continue with unauthenticated request
    }

    filterChain.doFilter(request, response);
  }
}
