package com.example.customers.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter to add correlation ID and user information to MDC for logging. This ensures all log
 * entries for a specific request can be traced together.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final String USER_ID_MDC_KEY = "userId";
  private static final String USERNAME_MDC_KEY = "username";
  private static final String REQUEST_URI_MDC_KEY = "requestUri";
  private static final String REQUEST_METHOD_MDC_KEY = "requestMethod";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = getOrGenerateCorrelationId(request);

    try {
      // Add correlation ID to MDC
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

      // Add request information to MDC
      MDC.put(REQUEST_URI_MDC_KEY, request.getRequestURI());
      MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());

      // Add user information to MDC if authenticated
      addUserInfoToMDC();

      // Add correlation ID to response header
      response.setHeader(CORRELATION_ID_HEADER, correlationId);

      // Log request start
      if (logger.isDebugEnabled()) {
        logger.debug("Request started: {} {}", request.getMethod(), request.getRequestURI());
      }

      // Continue with the filter chain
      filterChain.doFilter(request, response);

      // Log request completion
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Request completed: {} {} - Status: {}",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus());
      }

    } finally {
      // Clean up MDC to prevent memory leaks and thread contamination
      MDC.remove(CORRELATION_ID_MDC_KEY);
      MDC.remove(USER_ID_MDC_KEY);
      MDC.remove(USERNAME_MDC_KEY);
      MDC.remove(REQUEST_URI_MDC_KEY);
      MDC.remove(REQUEST_METHOD_MDC_KEY);
      MDC.clear();
    }
  }

  /** Extract correlation ID from request header or generate a new one. */
  private String getOrGenerateCorrelationId(HttpServletRequest request) {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = generateCorrelationId();
    }

    return correlationId;
  }

  /** Generate a new correlation ID using UUID. */
  private String generateCorrelationId() {
    return UUID.randomUUID().toString();
  }

  /** Add user information to MDC if user is authenticated. */
  private void addUserInfoToMDC() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {

      String username = authentication.getName();
      MDC.put(USERNAME_MDC_KEY, username);

      // Try to get user ID from authentication details
      Object userId = authentication.getPrincipal();
      if (userId != null) {
        MDC.put(USER_ID_MDC_KEY, userId.toString());
      }

      logger.trace("User context added to MDC: {}", username);
    }
  }
}
