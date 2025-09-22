package com.example.customers.service;

import com.example.customers.model.ChatMessage;
import com.example.customers.model.ChatSession;
import com.example.customers.model.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket service for real-time chat functionality.
 * 
 * <p>Handles broadcasting messages, typing indicators, and online presence
 * to connected WebSocket clients in real-time.
 */
@Service
public class ChatWebSocketService {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  // Track online users by session ID
  private final Map<Long, Map<String, LocalDateTime>> onlineUsers = new ConcurrentHashMap<>();

  /**
   * Broadcast a new message to all participants in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param message the message to broadcast
   */
  public void broadcastMessage(Long chatSessionId, ChatMessage message) {
    String destination = "/topic/chat." + chatSessionId + ".messages";
    messagingTemplate.convertAndSend(destination, message);
  }

  /**
   * Broadcast a typing indicator to other participants in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param userPhone the phone number of the user who is typing
   * @param isTyping true if user is typing, false if user stopped typing
   */
  public void broadcastTypingIndicator(Long chatSessionId, String userPhone, boolean isTyping) {
    String destination = "/topic/chat." + chatSessionId + ".typing";
    
    Map<String, Object> typingData = Map.of(
        "userPhone", userPhone,
        "isTyping", isTyping,
        "timestamp", LocalDateTime.now()
    );
    
    messagingTemplate.convertAndSend(destination, typingData);
  }

  /**
   * Broadcast online presence to all participants in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param userPhone the phone number of the user
   * @param isOnline true if user is online, false if offline
   */
  public void broadcastPresence(Long chatSessionId, String userPhone, boolean isOnline) {
    String destination = "/topic/chat." + chatSessionId + ".presence";
    
    Map<String, Object> presenceData = Map.of(
        "userPhone", userPhone,
        "isOnline", isOnline,
        "timestamp", LocalDateTime.now()
    );
    
    messagingTemplate.convertAndSend(destination, presenceData);
  }

  /**
   * Broadcast read receipt to message sender.
   * 
   * @param chatSessionId the chat session ID
   * @param messageId the message ID that was read
   * @param readerPhone the phone number of the user who read the message
   */
  public void broadcastReadReceipt(Long chatSessionId, Long messageId, String readerPhone) {
    String destination = "/topic/chat." + chatSessionId + ".read";
    
    Map<String, Object> readData = Map.of(
        "messageId", messageId,
        "readerPhone", readerPhone,
        "timestamp", LocalDateTime.now()
    );
    
    messagingTemplate.convertAndSend(destination, readData);
  }

  /**
   * Send a private message to a specific user.
   * 
   * @param userPhone the phone number of the target user
   * @param message the message to send
   */
  public void sendPrivateMessage(String userPhone, Object message) {
    String destination = "/user/" + userPhone + "/queue/messages";
    messagingTemplate.convertAndSend(destination, message);
  }

  /**
   * Mark a user as online in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param userPhone the phone number of the user
   */
  public void markUserOnline(Long chatSessionId, String userPhone) {
    onlineUsers.computeIfAbsent(chatSessionId, k -> new ConcurrentHashMap<>())
               .put(userPhone, LocalDateTime.now());
    
    broadcastPresence(chatSessionId, userPhone, true);
  }

  /**
   * Mark a user as offline in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param userPhone the phone number of the user
   */
  public void markUserOffline(Long chatSessionId, String userPhone) {
    Map<String, LocalDateTime> sessionUsers = onlineUsers.get(chatSessionId);
    if (sessionUsers != null) {
      sessionUsers.remove(userPhone);
      if (sessionUsers.isEmpty()) {
        onlineUsers.remove(chatSessionId);
      }
    }
    
    broadcastPresence(chatSessionId, userPhone, false);
  }

  /**
   * Get online users for a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @return map of online users with their last activity time
   */
  public Map<String, LocalDateTime> getOnlineUsers(Long chatSessionId) {
    return onlineUsers.getOrDefault(chatSessionId, Map.of());
  }

  /**
   * Check if a user is online in a chat session.
   * 
   * @param chatSessionId the chat session ID
   * @param userPhone the phone number of the user
   * @return true if user is online, false otherwise
   */
  public boolean isUserOnline(Long chatSessionId, String userPhone) {
    Map<String, LocalDateTime> sessionUsers = onlineUsers.get(chatSessionId);
    if (sessionUsers == null) {
      return false;
    }
    
    LocalDateTime lastActivity = sessionUsers.get(userPhone);
    if (lastActivity == null) {
      return false;
    }
    
    // Consider user offline if last activity was more than 5 minutes ago
    return lastActivity.isAfter(LocalDateTime.now().minusMinutes(5));
  }
}