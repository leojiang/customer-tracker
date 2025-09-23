package com.example.customers.repository;

import com.example.customers.model.ChatSession;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ChatSession entity operations.
 *
 * <p>Provides methods for finding chat sessions by participants and retrieving paginated lists of
 * sessions for a user.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

  /**
   * Find a chat session between two participants.
   *
   * @param participant1Phone first participant's phone number
   * @param participant2Phone second participant's phone number
   * @return Optional ChatSession if found
   */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE "
          + "(cs.participant1Phone = :participant1 AND cs.participant2Phone = :participant2) OR "
          + "(cs.participant1Phone = :participant2 AND cs.participant2Phone = :participant1)")
  Optional<ChatSession> findByParticipants(
      @Param("participant1") String participant1Phone,
      @Param("participant2") String participant2Phone);

  /**
   * Find all chat sessions for a user, ordered by last message time.
   *
   * @param userPhone the user's phone number
   * @param pageable pagination parameters
   * @return Page of ChatSessions
   */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE "
          + "cs.participant1Phone = :userPhone OR cs.participant2Phone = :userPhone "
          + "ORDER BY COALESCE(cs.lastMessageAt, cs.createdAt) DESC")
  Page<ChatSession> findByUserPhoneOrderByLastMessage(
      @Param("userPhone") String userPhone, Pageable pageable);

  /**
   * Find all chat sessions for a user (without pagination).
   *
   * @param userPhone the user's phone number
   * @return List of ChatSessions
   */
  @Query(
      "SELECT cs FROM ChatSession cs WHERE "
          + "cs.participant1Phone = :userPhone OR cs.participant2Phone = :userPhone "
          + "ORDER BY COALESCE(cs.lastMessageAt, cs.createdAt) DESC")
  java.util.List<ChatSession> findByUserPhone(@Param("userPhone") String userPhone);

  /**
   * Check if a chat session exists between two participants.
   *
   * @param participant1Phone first participant's phone number
   * @param participant2Phone second participant's phone number
   * @return true if session exists
   */
  @Query(
      "SELECT COUNT(cs) > 0 FROM ChatSession cs WHERE "
          + "(cs.participant1Phone = :participant1 AND cs.participant2Phone = :participant2) OR "
          + "(cs.participant1Phone = :participant2 AND cs.participant2Phone = :participant1)")
  boolean existsByParticipants(
      @Param("participant1") String participant1Phone,
      @Param("participant2") String participant2Phone);
}
