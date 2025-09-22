package com.example.customers.controller;

import com.example.customers.model.ChatMessage;
import com.example.customers.model.ChatSession;
import com.example.customers.model.Sales;
import com.example.customers.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat operations.
 *
 * <p>Provides endpoints for 1-to-1 chat sessions and messages between sales users.
 */
@Tag(name = "Chat", description = "APIs for instant messaging between users")
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer Authentication")
public class ChatController {

  private final ChatService chatService;

  @Autowired
  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  /**
   * Get or create a chat session with another user.
   *
   * @param otherUserPhone the other user's phone number
   * @param authentication the current user's authentication
   * @return ResponseEntity containing the ChatSession
   */
  @Operation(
      summary = "Get or create chat session with another user",
      description = "Creates a new chat session if one doesn't exist, or returns existing session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chat session retrieved or created successfully",
            content = @Content(schema = @Schema(implementation = ChatSession.class))),
        @ApiResponse(responseCode = "400", description = "Invalid user phone number"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping("/sessions")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<ChatSession> getOrCreateChatSession(
      @Parameter(description = "Other user's phone number") @RequestParam String otherUserPhone,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();
    
    if (currentUserPhone.equals(otherUserPhone)) {
      return ResponseEntity.badRequest().build();
    }

    try {
      ChatSession session = chatService.getOrCreateChatSession(currentUserPhone, otherUserPhone);
      return ResponseEntity.ok(session);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get chat sessions for the current user.
   *
   * @param page page number (0-based)
   * @param size page size
   * @param authentication the current user's authentication
   * @return ResponseEntity containing Page of ChatSessions
   */
  @Operation(
      summary = "Get user's chat sessions",
      description = "Retrieve paginated list of chat sessions for the current user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chat sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sessions")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<Page<ChatSession>> getChatSessions(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();
    Page<ChatSession> sessions = chatService.getChatSessions(currentUserPhone, page, size);
    return ResponseEntity.ok(sessions);
  }

  /**
   * Get all chat sessions for the current user (without pagination).
   *
   * @param authentication the current user's authentication
   * @return ResponseEntity containing List of ChatSessions
   */
  @Operation(
      summary = "Get all user's chat sessions",
      description = "Retrieve all chat sessions for the current user without pagination")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chat sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sessions/all")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<List<ChatSession>> getAllChatSessions(Authentication authentication) {
    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();
    List<ChatSession> sessions = chatService.getAllChatSessions(currentUserPhone);
    return ResponseEntity.ok(sessions);
  }

  /**
   * Send a message in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param request the message request
   * @param authentication the current user's authentication
   * @return ResponseEntity containing the created ChatMessage
   */
  @Operation(
      summary = "Send a message",
      description = "Send a message in a chat session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = ChatMessage.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or user not participant"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping("/sessions/{chatSessionId}/messages")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<ChatMessage> sendMessage(
      @Parameter(description = "Chat session ID") @PathVariable Long chatSessionId,
      @Valid @RequestBody SendMessageRequest request,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      ChatMessage message = chatService.sendMessage(chatSessionId, currentUserPhone, request.getMessageContent());
      return ResponseEntity.ok(message);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get messages in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param page page number (0-based)
   * @param size page size
   * @param authentication the current user's authentication
   * @return ResponseEntity containing Page of ChatMessages
   */
  @Operation(
      summary = "Get messages in chat session",
      description = "Retrieve paginated messages in a chat session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "400", description = "Invalid session or user not participant"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sessions/{chatSessionId}/messages")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<Page<ChatMessage>> getMessages(
      @Parameter(description = "Chat session ID") @PathVariable Long chatSessionId,
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      Page<ChatMessage> messages = chatService.getMessages(chatSessionId, currentUserPhone, page, size);
      return ResponseEntity.ok(messages);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get all messages in a chat session (without pagination).
   *
   * @param chatSessionId the chat session ID
   * @param authentication the current user's authentication
   * @return ResponseEntity containing List of ChatMessages
   */
  @Operation(
      summary = "Get all messages in chat session",
      description = "Retrieve all messages in a chat session without pagination")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "400", description = "Invalid session or user not participant"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sessions/{chatSessionId}/messages/all")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<List<ChatMessage>> getAllMessages(
      @Parameter(description = "Chat session ID") @PathVariable Long chatSessionId,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      List<ChatMessage> messages = chatService.getAllMessages(chatSessionId, currentUserPhone);
      return ResponseEntity.ok(messages);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Mark messages as read in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param authentication the current user's authentication
   * @return ResponseEntity with success status
   */
  @Operation(
      summary = "Mark messages as read",
      description = "Mark all unread messages in a chat session as read")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Messages marked as read successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid session or user not participant"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping("/sessions/{chatSessionId}/read")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<Void> markMessagesAsRead(
      @Parameter(description = "Chat session ID") @PathVariable Long chatSessionId,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      chatService.markMessagesAsRead(chatSessionId, currentUserPhone);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get unread message count for the current user.
   *
   * @param authentication the current user's authentication
   * @return ResponseEntity containing the unread count
   */
  @Operation(
      summary = "Get unread message count",
      description = "Get total unread message count for the current user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/unread-count")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();
    long unreadCount = chatService.getUnreadMessageCount(currentUserPhone);
    return ResponseEntity.ok(new UnreadCountResponse(unreadCount));
  }

  /**
   * Get the other participant in a chat session.
   *
   * @param chatSessionId the chat session ID
   * @param authentication the current user's authentication
   * @return ResponseEntity containing the other participant's information
   */
  @Operation(
      summary = "Get other participant",
      description = "Get the other participant's information in a chat session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Other participant retrieved successfully",
            content = @Content(schema = @Schema(implementation = Sales.class))),
        @ApiResponse(responseCode = "400", description = "Invalid session or user not participant"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sessions/{chatSessionId}/other-participant")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<Sales> getOtherParticipant(
      @Parameter(description = "Chat session ID") @PathVariable Long chatSessionId,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      return chatService.getOtherParticipant(chatSessionId, currentUserPhone)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Search for users to start a chat with.
   *
   * @param query search query (phone number or name)
   * @param authentication the current user's authentication
   * @return ResponseEntity containing list of users
   */
  @Operation(
      summary = "Search users for chat",
      description = "Search for other users to start a chat session with")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/users/search")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
  public ResponseEntity<List<Sales>> searchUsers(
      @Parameter(description = "Search query (phone or name)") @RequestParam String query,
      Authentication authentication) {

    // Extract phone from Sales object (principal)
    Sales currentUser = (Sales) authentication.getPrincipal();
    String currentUserPhone = currentUser.getPhone();

    try {
      List<Sales> users = chatService.searchUsers(query, currentUserPhone);
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  // Request/Response DTOs
  public static class SendMessageRequest {
    @NotBlank(message = "Message content cannot be blank")
    private String messageContent;

    public String getMessageContent() {
      return messageContent;
    }

    public void setMessageContent(String messageContent) {
      this.messageContent = messageContent;
    }
  }

  public static class UnreadCountResponse {
    private long unreadCount;

    public UnreadCountResponse(long unreadCount) {
      this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
      return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
      this.unreadCount = unreadCount;
    }
  }
}