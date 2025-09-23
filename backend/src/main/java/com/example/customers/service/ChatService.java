package com.example.customers.service;

import com.example.customers.model.ChatMessage;
import com.example.customers.model.ChatSession;
import com.example.customers.model.Sales;
import com.example.customers.repository.ChatMessageRepository;
import com.example.customers.repository.ChatSessionRepository;
import com.example.customers.repository.SalesRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for chat operations.
 *
 * <p>Handles business logic for 1-to-1 chat sessions and messages between sales users.
 */
@Service
@Transactional
public class ChatService {

  private final ChatSessionRepository chatSessionRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SalesRepository salesRepository;
  @Autowired
  public ChatService(
      ChatSessionRepository chatSessionRepository,
      ChatMessageRepository chatMessageRepository,
      SalesRepository salesRepository) {
    this.chatSessionRepository = chatSessionRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.salesRepository = salesRepository;
  }

  /**
   * Get or create a chat session between two users.
   *
   * @param user1Phone first user's phone number
   * @param user2Phone second user's phone number
   * @return ChatSession between the two users
   * @throws IllegalArgumentException if either user doesn't exist
   */
  public ChatSession getOrCreateChatSession(String user1Phone, String user2Phone) {
    // Validate that both users exist
    if (!salesRepository.existsByPhone(user1Phone)) {
      throw new IllegalArgumentException("User with phone " + user1Phone + " does not exist");
    }
    if (!salesRepository.existsByPhone(user2Phone)) {
      throw new IllegalArgumentException("User with phone " + user2Phone + " does not exist");
    }

    // Check if session already exists
    Optional<ChatSession> existingSession =
        chatSessionRepository.findByParticipants(user1Phone, user2Phone);
    if (existingSession.isPresent()) {
      return existingSession.get();
    }

    // Create new session
    ChatSession newSession = new ChatSession(user1Phone, user2Phone);
    return chatSessionRepository.save(newSession);
  }

  /**
   * Send a message in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param senderPhone the sender's phone number
   * @param messageContent the message content
   * @return the created ChatMessage
   * @throws IllegalArgumentException if session doesn't exist or user is not a participant
   */
  public ChatMessage sendMessage(Long chatSessionId, String senderPhone, String messageContent) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(senderPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    ChatMessage message = new ChatMessage(session, senderPhone, messageContent);
    ChatMessage savedMessage = chatMessageRepository.save(message);

    // Update session's last message info (handled by database trigger)
    session.setUpdatedAt(LocalDateTime.now());
    chatSessionRepository.save(session);

    // No WebSocket broadcasting needed - using HTTP polling instead

    return savedMessage;
  }

  /**
   * Get chat sessions for a user with pagination.
   *
   * @param userPhone the user's phone number
   * @param page page number (0-based)
   * @param size page size
   * @return Page of ChatSessions
   */
  @Transactional(readOnly = true)
  public Page<ChatSession> getChatSessions(String userPhone, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return chatSessionRepository.findByUserPhoneOrderByLastMessage(userPhone, pageable);
  }

  /**
   * Get all chat sessions for a user.
   *
   * @param userPhone the user's phone number
   * @return List of ChatSessions
   */
  @Transactional(readOnly = true)
  public List<ChatSession> getAllChatSessions(String userPhone) {
    return chatSessionRepository.findByUserPhone(userPhone);
  }

  /**
   * Get messages in a chat session with pagination.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number (for validation)
   * @param page page number (0-based)
   * @param size page size
   * @return Page of ChatMessages
   * @throws IllegalArgumentException if session doesn't exist or user is not a participant
   */
  @Transactional(readOnly = true)
  public Page<ChatMessage> getMessages(Long chatSessionId, String userPhone, int page, int size) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(userPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    Pageable pageable = PageRequest.of(page, size);
    return chatMessageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId, pageable);
  }

  /**
   * Get all messages in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number (for validation)
   * @return List of ChatMessages
   * @throws IllegalArgumentException if session doesn't exist or user is not a participant
   */
  @Transactional(readOnly = true)
  public List<ChatMessage> getAllMessages(Long chatSessionId, String userPhone) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(userPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    return chatMessageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId);
  }

  /**
   * Mark messages as read in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number
   * @throws IllegalArgumentException if session doesn't exist or user is not a participant
   */
  public void markMessagesAsRead(Long chatSessionId, String userPhone) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(userPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    chatMessageRepository.markMessagesAsReadInSession(
        chatSessionId, userPhone, LocalDateTime.now());
  }

  /**
   * Get unread message count for a user across all chat sessions.
   *
   * @param userPhone the user's phone number
   * @return count of unread messages
   */
  @Transactional(readOnly = true)
  public long getUnreadMessageCount(String userPhone) {
    return chatMessageRepository.countTotalUnreadMessages(userPhone);
  }

  /**
   * Get unread message count for a user in a specific chat session.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number
   * @return count of unread messages
   * @throws IllegalArgumentException if session doesn't exist or user is not a participant
   */
  @Transactional(readOnly = true)
  public long getUnreadMessageCountInSession(Long chatSessionId, String userPhone) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(userPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    return chatMessageRepository.countUnreadMessagesInSession(chatSessionId, userPhone);
  }

  /**
   * Get the other participant's information in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param currentUserPhone the current user's phone number
   * @return Optional Sales user (the other participant)
   */
  @Transactional(readOnly = true)
  public Optional<Sales> getOtherParticipant(Long chatSessionId, String currentUserPhone) {
    ChatSession session =
        chatSessionRepository
            .findById(chatSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));

    if (!session.isParticipant(currentUserPhone)) {
      throw new IllegalArgumentException("User is not a participant in this chat session");
    }

    String otherParticipantPhone = session.getOtherParticipantPhone(currentUserPhone);
    return salesRepository.findByPhone(otherParticipantPhone);
  }

  /**
   * Search for users to start a chat with.
   *
   * @param query search query (phone number or name)
   * @param currentUserPhone the current user's phone number (to exclude from results)
   * @return List of Sales users matching the search query
   */
  @Transactional(readOnly = true)
  public List<Sales> searchUsers(String query, String currentUserPhone) {
    if (query == null || query.trim().isEmpty()) {
      return List.of();
    }

    String trimmedQuery = query.trim();

    // Search by phone number (exact match or partial match)
    List<Sales> phoneMatches = salesRepository.findByPhoneContainingIgnoreCase(trimmedQuery);

    // Search by name (if users have names stored - currently they don't, but for future
    // extensibility)
    // For now, we'll only search by phone since Sales users are identified by phone numbers

    // Filter out the current user and only return approved users
    return phoneMatches.stream()
        .filter(user -> !user.getPhone().equals(currentUserPhone))
        .filter(
            user -> user.getApprovalStatus() == com.example.customers.model.ApprovalStatus.APPROVED)
        .filter(user -> !user.isDisabled())
        .limit(10) // Limit results to 10 users
        .collect(java.util.stream.Collectors.toList());
  }
}
