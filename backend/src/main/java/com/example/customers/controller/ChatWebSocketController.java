package com.example.customers.controller;

import com.example.customers.model.ChatMessage;
import com.example.customers.model.ChatSession;
import com.example.customers.model.Sales;
import com.example.customers.service.ChatService;
import com.example.customers.service.ChatWebSocketService;
import com.example.customers.repository.ChatMessageRepository;
import com.example.customers.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for real-time chat functionality.
 * 
 * <p>Handles incoming WebSocket messages for chat operations including
 * sending messages, typing indicators, and presence management.
 */
@Controller
public class ChatWebSocketController {

  @Autowired
  private ChatService chatService;

  @Autowired
  private ChatWebSocketService webSocketService;

  @Autowired
  private ChatSessionRepository chatSessionRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  /**
   * Handle incoming chat messages via WebSocket.
   * 
   * @param chatSessionId the chat session ID
   * @param messageData the message data containing content
   * @param headerAccessor WebSocket header accessor for authentication
   */
  @MessageMapping("/chat/{chatSessionId}/message")
  public void handleMessage(
      @DestinationVariable Long chatSessionId,
      @Payload Map<String, Object> messageData,
      SimpMessageHeaderAccessor headerAccessor) {

    try {
      // Get authenticated user
      Authentication auth = (Authentication) headerAccessor.getUser();
      Sales currentUser = (Sales) auth.getPrincipal();
      String currentUserPhone = currentUser.getPhone();

      // Validate chat session and user participation
      ChatSession session = chatSessionRepository.findById(chatSessionId)
          .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

      if (!session.isParticipant(currentUserPhone)) {
        throw new IllegalArgumentException("User is not a participant in this chat session");
      }

      // Extract message content
      String messageContent = (String) messageData.get("content");
      if (messageContent == null || messageContent.trim().isEmpty()) {
        throw new IllegalArgumentException("Message content cannot be empty");
      }

      // Send message via ChatService (saves to database)
      ChatMessage message = chatService.sendMessage(chatSessionId, currentUserPhone, messageContent.trim());

      // Broadcast message to all participants via WebSocket
      webSocketService.broadcastMessage(chatSessionId, message);

    } catch (Exception e) {
      // Send error message back to sender
      try {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
          Sales currentUser = (Sales) auth.getPrincipal();
          Map<String, Object> errorResponse = Map.of(
              "error", "Failed to send message: " + e.getMessage(),
              "timestamp", System.currentTimeMillis()
          );
          
          webSocketService.sendPrivateMessage(currentUser.getPhone(), errorResponse);
        }
      } catch (Exception ex) {
        // Log error but don't fail
        System.err.println("Error sending error message: " + ex.getMessage());
      }
    }
  }

  /**
   * Handle typing indicators.
   * 
   * @param chatSessionId the chat session ID
   * @param typingData the typing data containing typing status
   * @param headerAccessor WebSocket header accessor for authentication
   */
  @MessageMapping("/chat/{chatSessionId}/typing")
  public void handleTyping(
      @DestinationVariable Long chatSessionId,
      @Payload Map<String, Object> typingData,
      SimpMessageHeaderAccessor headerAccessor) {

    try {
      // Get authenticated user
      Authentication auth = (Authentication) headerAccessor.getUser();
      Sales currentUser = (Sales) auth.getPrincipal();
      String currentUserPhone = currentUser.getPhone();

      // Validate chat session and user participation
      ChatSession session = chatSessionRepository.findById(chatSessionId)
          .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

      if (!session.isParticipant(currentUserPhone)) {
        throw new IllegalArgumentException("User is not a participant in this chat session");
      }

      // Extract typing status
      Boolean isTyping = (Boolean) typingData.get("isTyping");
      if (isTyping == null) {
        throw new IllegalArgumentException("Typing status is required");
      }

      // Broadcast typing indicator to other participants
      webSocketService.broadcastTypingIndicator(chatSessionId, currentUserPhone, isTyping);

    } catch (Exception e) {
      // Log error but don't send back to client for typing indicators
      System.err.println("Error handling typing indicator: " + e.getMessage());
    }
  }

  /**
   * Handle user joining a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param headerAccessor WebSocket header accessor for authentication
   */
  @MessageMapping("/chat/{chatSessionId}/join")
  public void handleJoin(
      @DestinationVariable Long chatSessionId,
      SimpMessageHeaderAccessor headerAccessor) {

    try {
      // Get authenticated user
      Authentication auth = (Authentication) headerAccessor.getUser();
      Sales currentUser = (Sales) auth.getPrincipal();
      String currentUserPhone = currentUser.getPhone();

      // Validate chat session and user participation
      ChatSession session = chatSessionRepository.findById(chatSessionId)
          .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

      if (!session.isParticipant(currentUserPhone)) {
        throw new IllegalArgumentException("User is not a participant in this chat session");
      }

      // Mark user as online
      webSocketService.markUserOnline(chatSessionId, currentUserPhone);

    } catch (Exception e) {
      System.err.println("Error handling join: " + e.getMessage());
    }
  }

  /**
   * Handle user leaving a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param headerAccessor WebSocket header accessor for authentication
   */
  @MessageMapping("/chat/{chatSessionId}/leave")
  public void handleLeave(
      @DestinationVariable Long chatSessionId,
      SimpMessageHeaderAccessor headerAccessor) {

    try {
      // Get authenticated user
      Authentication auth = (Authentication) headerAccessor.getUser();
      Sales currentUser = (Sales) auth.getPrincipal();
      String currentUserPhone = currentUser.getPhone();

      // Mark user as offline
      webSocketService.markUserOffline(chatSessionId, currentUserPhone);

    } catch (Exception e) {
      System.err.println("Error handling leave: " + e.getMessage());
    }
  }

  /**
   * Handle read receipts.
   * 
   * @param chatSessionId the chat session ID
   * @param readData the read data containing message ID
   * @param headerAccessor WebSocket header accessor for authentication
   */
  @MessageMapping("/chat/{chatSessionId}/read")
  public void handleReadReceipt(
      @DestinationVariable Long chatSessionId,
      @Payload Map<String, Object> readData,
      SimpMessageHeaderAccessor headerAccessor) {

    try {
      // Get authenticated user
      Authentication auth = (Authentication) headerAccessor.getUser();
      Sales currentUser = (Sales) auth.getPrincipal();
      String currentUserPhone = currentUser.getPhone();

      // Validate chat session and user participation
      ChatSession session = chatSessionRepository.findById(chatSessionId)
          .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

      if (!session.isParticipant(currentUserPhone)) {
        throw new IllegalArgumentException("User is not a participant in this chat session");
      }

      // Extract message ID
      Long messageId = Long.valueOf(readData.get("messageId").toString());

      // Validate message exists and user is participant
      ChatMessage message = chatMessageRepository.findById(messageId)
          .orElseThrow(() -> new IllegalArgumentException("Message not found"));

      if (!message.getChatSession().getId().equals(chatSessionId)) {
        throw new IllegalArgumentException("Message does not belong to this chat session");
      }

      // Broadcast read receipt
      webSocketService.broadcastReadReceipt(chatSessionId, messageId, currentUserPhone);

    } catch (Exception e) {
      System.err.println("Error handling read receipt: " + e.getMessage());
    }
  }
}