package com.example.customers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a chat session between two users.
 *
 * <p>Each chat session represents a 1-to-1 conversation between two sales users. The session is
 * identified by the combination of participant phone numbers.
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "participant1_phone", nullable = false, length = 20)
  private String participant1Phone;

  @Column(name = "participant2_phone", nullable = false, length = 20)
  private String participant2Phone;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  @Column(name = "last_message_preview", length = 200)
  private String lastMessagePreview;

  // Removed bidirectional relationship to avoid circular dependency issues
  // Messages can be accessed through ChatMessageRepository

  // Constructors
  public ChatSession() {}

  public ChatSession(String participant1Phone, String participant2Phone) {
    this.participant1Phone = participant1Phone;
    this.participant2Phone = participant2Phone;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getParticipant1Phone() {
    return participant1Phone;
  }

  public void setParticipant1Phone(String participant1Phone) {
    this.participant1Phone = participant1Phone;
  }

  public String getParticipant2Phone() {
    return participant2Phone;
  }

  public void setParticipant2Phone(String participant2Phone) {
    this.participant2Phone = participant2Phone;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getLastMessageAt() {
    return lastMessageAt;
  }

  public void setLastMessageAt(LocalDateTime lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
  }

  public String getLastMessagePreview() {
    return lastMessagePreview;
  }

  public void setLastMessagePreview(String lastMessagePreview) {
    this.lastMessagePreview = lastMessagePreview;
  }

  // Messages access removed - use ChatMessageRepository instead

  /**
   * Get the other participant's phone number.
   *
   * @param currentUserPhone the current user's phone number
   * @return the other participant's phone number
   */
  public String getOtherParticipantPhone(String currentUserPhone) {
    if (participant1Phone.equals(currentUserPhone)) {
      return participant2Phone;
    } else {
      return participant1Phone;
    }
  }

  /**
   * Check if the given phone number is a participant in this session.
   *
   * @param phone the phone number to check
   * @return true if the phone number is a participant
   */
  public boolean isParticipant(String phone) {
    return participant1Phone.equals(phone) || participant2Phone.equals(phone);
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
