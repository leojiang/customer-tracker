package com.example.customers.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                      AuthenticationException authException) throws IOException {
    
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "Unauthorized");
    errorResponse.put("message", "Invalid or missing authentication token");
    errorResponse.put("path", request.getRequestURI());
    errorResponse.put("timestamp", java.time.Instant.now().toString());
    
    ObjectMapper mapper = new ObjectMapper();
    String jsonResponse = mapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }
}