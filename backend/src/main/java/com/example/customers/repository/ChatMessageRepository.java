package com.example.customers.repository;

import com.example.customers.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ChatMessage entity operations.
 *
 * <p>Provides methods for finding messages by chat session and managing
 * message read status.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /**
   * Find all messages in a chat session, ordered by sent time.
   *
   * @param chatSessionId the chat session ID
   * @param pageable pagination parameters
   * @return Page of ChatMessages
   */
  Page<ChatMessage> findByChatSessionIdOrderBySentAtAsc(
      @Param("chatSessionId") Long chatSessionId,
      Pageable pageable);

  /**
   * Find all messages in a chat session (without pagination).
   *
   * @param chatSessionId the chat session ID
   * @return List of ChatMessages
   */
  List<ChatMessage> findByChatSessionIdOrderBySentAtAsc(@Param("chatSessionId") Long chatSessionId);

  /**
   * Find unread messages for a user in a specific chat session.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number (to exclude their own messages)
   * @return List of unread ChatMessages
   */
  @Query("SELECT cm FROM ChatMessage cm WHERE " +
         "cm.chatSession.id = :chatSessionId AND " +
         "cm.senderPhone != :userPhone AND " +
         "cm.isRead = false " +
         "ORDER BY cm.sentAt ASC")
  List<ChatMessage> findUnreadMessagesInSession(
      @Param("chatSessionId") Long chatSessionId,
      @Param("userPhone") String userPhone);

  /**
   * Count unread messages for a user in a specific chat session.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number (to exclude their own messages)
   * @return count of unread messages
   */
  @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE " +
         "cm.chatSession.id = :chatSessionId AND " +
         "cm.senderPhone != :userPhone AND " +
         "cm.isRead = false")
  long countUnreadMessagesInSession(
      @Param("chatSessionId") Long chatSessionId,
      @Param("userPhone") String userPhone);

  /**
   * Count total unread messages for a user across all chat sessions.
   *
   * @param userPhone the user's phone number (to exclude their own messages)
   * @return count of unread messages
   */
  @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
         "JOIN cm.chatSession cs WHERE " +
         "(cs.participant1Phone = :userPhone OR cs.participant2Phone = :userPhone) AND " +
         "cm.senderPhone != :userPhone AND " +
         "cm.isRead = false")
  long countTotalUnreadMessages(@Param("userPhone") String userPhone);

  /**
   * Mark messages as read in a chat session for a specific user.
   *
   * @param chatSessionId the chat session ID
   * @param userPhone the user's phone number (to exclude their own messages)
   * @param readAt the timestamp when messages were read
   */
  @Modifying
  @Query("UPDATE ChatMessage cm SET " +
         "cm.isRead = true, " +
         "cm.readAt = :readAt " +
         "WHERE cm.chatSession.id = :chatSessionId AND " +
         "cm.senderPhone != :userPhone AND " +
         "cm.isRead = false")
  void markMessagesAsReadInSession(
      @Param("chatSessionId") Long chatSessionId,
      @Param("userPhone") String userPhone,
      @Param("readAt") LocalDateTime readAt);

  /**
   * Find the latest message in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @return Optional ChatMessage (latest one)
   */
  @Query("SELECT cm FROM ChatMessage cm WHERE " +
         "cm.chatSession.id = :chatSessionId " +
         "ORDER BY cm.sentAt DESC")
  java.util.Optional<ChatMessage> findLatestMessageInSession(@Param("chatSessionId") Long chatSessionId);
}