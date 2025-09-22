import { ChatSession, ChatMessage, UnreadCountResponse } from '@/types/chat';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

class ChatApiService {
  private async getAuthHeaders(): Promise<HeadersInit> {
    const token = localStorage.getItem('auth_token');
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    };
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }

  /**
   * Get or create a chat session with another user
   */
  async getOrCreateChatSession(otherUserPhone: string): Promise<ChatSession> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions?otherUserPhone=${encodeURIComponent(otherUserPhone)}`, {
      method: 'POST',
      headers: await this.getAuthHeaders(),
    });
    return this.handleResponse<ChatSession>(response);
  }

  /**
   * Get chat sessions for the current user with pagination
   */
  async getChatSessions(page: number = 0, size: number = 20): Promise<{
    content: ChatSession[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }> {
    const response = await fetch(
      `${API_BASE_URL}/chat/sessions?page=${page}&size=${size}`,
      {
        headers: await this.getAuthHeaders(),
      }
    );
    return this.handleResponse(response);
  }

  /**
   * Get all chat sessions for the current user (without pagination)
   */
  async getAllChatSessions(): Promise<ChatSession[]> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions/all`, {
      headers: await this.getAuthHeaders(),
    });
    return this.handleResponse<ChatSession[]>(response);
  }

  /**
   * Send a message in a chat session
   */
  async sendMessage(chatSessionId: number, messageContent: string): Promise<ChatMessage> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions/${chatSessionId}/messages`, {
      method: 'POST',
      headers: await this.getAuthHeaders(),
      body: JSON.stringify({ messageContent }),
    });
    return this.handleResponse<ChatMessage>(response);
  }

  /**
   * Get messages in a chat session with pagination
   */
  async getMessages(chatSessionId: number, page: number = 0, size: number = 50): Promise<{
    content: ChatMessage[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }> {
    const response = await fetch(
      `${API_BASE_URL}/chat/sessions/${chatSessionId}/messages?page=${page}&size=${size}`,
      {
        headers: await this.getAuthHeaders(),
      }
    );
    return this.handleResponse(response);
  }

  /**
   * Get all messages in a chat session (without pagination)
   */
  async getAllMessages(chatSessionId: number): Promise<ChatMessage[]> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions/${chatSessionId}/messages/all`, {
      headers: await this.getAuthHeaders(),
    });
    return this.handleResponse<ChatMessage[]>(response);
  }

  /**
   * Mark messages as read in a chat session
   */
  async markMessagesAsRead(chatSessionId: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions/${chatSessionId}/read`, {
      method: 'POST',
      headers: await this.getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
  }

  /**
   * Get unread message count for the current user
   */
  async getUnreadCount(): Promise<number> {
    const response = await fetch(`${API_BASE_URL}/chat/unread-count`, {
      headers: await this.getAuthHeaders(),
    });
    const result = await this.handleResponse<UnreadCountResponse>(response);
    return result.unreadCount;
  }

  /**
   * Get the other participant in a chat session
   */
  async getOtherParticipant(chatSessionId: number): Promise<{
    phone: string;
    role: string;
  }> {
    const response = await fetch(`${API_BASE_URL}/chat/sessions/${chatSessionId}/other-participant`, {
      headers: await this.getAuthHeaders(),
    });
    return this.handleResponse(response);
  }

  /**
   * Search for users to start a chat with
   */
  async searchUsers(query: string): Promise<{
    phone: string;
    role: string;
  }[]> {
    const response = await fetch(`${API_BASE_URL}/chat/users/search?query=${encodeURIComponent(query)}`, {
      headers: await this.getAuthHeaders(),
    });
    return this.handleResponse(response);
  }
}

export const chatApi = new ChatApiService();