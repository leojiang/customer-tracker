package com.example.customers.config;

import com.example.customers.service.JwtService;
import com.example.customers.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket security configuration for JWT authentication.
 * 
 * <p>Intercepts WebSocket connection attempts and validates JWT tokens
 * to ensure only authenticated users can establish WebSocket connections.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  @Autowired
  private JwtService jwtService;

  @Autowired
  private AuthService authService;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          // Extract JWT token from Authorization header
          String authHeader = accessor.getFirstNativeHeader("Authorization");
          
          if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
              // Validate JWT token and extract phone number
              String phone = jwtService.extractPhone(token);
              
              if (phone != null && jwtService.isTokenValid(token, phone)) {
                // Get user details from database
                var salesOptional = authService.getSalesByPhone(phone);
                
                if (salesOptional.isPresent()) {
                  var sales = salesOptional.get();
                  
                  // Create authentication token
                  List<SimpleGrantedAuthority> authorities = 
                      List.of(new SimpleGrantedAuthority("ROLE_" + sales.getRole().name()));
                  
                  UsernamePasswordAuthenticationToken authToken = 
                      new UsernamePasswordAuthenticationToken(sales, null, authorities);
                  
                  // Set the authenticated user
                  accessor.setUser(authToken);
                }
              }
            } catch (Exception e) {
              // Token validation failed
              throw new IllegalArgumentException("Invalid JWT token");
            }
          } else {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
          }
        }
        
        return message;
      }
    });
  }
}