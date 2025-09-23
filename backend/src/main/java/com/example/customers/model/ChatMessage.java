package com.example.customers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a chat message within a chat session.
 *
 * <p>Each message belongs to a chat session and contains the sender's information, message content,
 * and timestamp.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_session_id", nullable = false)
  private ChatSession chatSession;

  @Column(name = "sender_phone", nullable = false, length = 20)
  private String senderPhone;

  @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
  private String messageContent;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  // Constructors
  public ChatMessage() {}

  public ChatMessage(ChatSession chatSession, String senderPhone, String messageContent) {
    this.chatSession = chatSession;
    this.senderPhone = senderPhone;
    this.messageContent = messageContent;
    this.sentAt = LocalDateTime.now();
    this.isRead = false;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ChatSession getChatSession() {
    return chatSession;
  }

  public void setChatSession(ChatSession chatSession) {
    this.chatSession = chatSession;
  }

  public String getSenderPhone() {
    return senderPhone;
  }

  public void setSenderPhone(String senderPhone) {
    this.senderPhone = senderPhone;
  }

  public String getMessageContent() {
    return messageContent;
  }

  public void setMessageContent(String messageContent) {
    this.messageContent = messageContent;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public Boolean getIsRead() {
    return isRead;
  }

  public void setIsRead(Boolean isRead) {
    this.isRead = isRead;
  }

  public LocalDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(LocalDateTime readAt) {
    this.readAt = readAt;
  }

  /** Mark the message as read. */
  public void markAsRead() {
    this.isRead = true;
    this.readAt = LocalDateTime.now();
  }

  /**
   * Check if the message was sent by the given user.
   *
   * @param userPhone the user's phone number
   * @return true if the message was sent by the user
   */
  public boolean isSentBy(String userPhone) {
    return senderPhone.equals(userPhone);
  }
}
