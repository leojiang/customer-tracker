package com.example.customers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time chat functionality.
 * 
 * <p>Configures STOMP message broker and WebSocket endpoints for chat communication.
 * Enables real-time messaging, typing indicators, and online presence features.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enable a simple in-memory message broker for destinations prefixed with "/topic"
    config.enableSimpleBroker("/topic");
    
    // Set the application destination prefix for messages sent from client to server
    config.setApplicationDestinationPrefixes("/app");
    
    // Set the user destination prefix for private messages
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Register the WebSocket endpoint that clients will connect to
    registry.addEndpoint("/ws/chat")
        .setAllowedOriginPatterns("*") // Allow all origins for development
        .withSockJS(); // Enable SockJS fallback for older browsers
  }
}