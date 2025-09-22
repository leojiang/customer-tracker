import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface WebSocketMessage {
  id: number;
  chatSession: {
    id: number;
  };
  senderPhone: string;
  messageContent: string;
  sentAt: string;
  isRead: boolean;
  readAt?: string;
}

export interface TypingIndicator {
  userPhone: string;
  isTyping: boolean;
  timestamp: string;
}

export interface PresenceUpdate {
  userPhone: string;
  isOnline: boolean;
  timestamp: string;
}

export interface ReadReceipt {
  messageId: number;
  readerPhone: string;
  timestamp: string;
}

export interface WebSocketError {
  error: string;
  timestamp: number;
}

class WebSocketService {
  private client: Client | null = null;
  private isConnected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 seconds
  private subscriptions: Map<string, { 
    messages: { unsubscribe: () => void }; 
    typing: { unsubscribe: () => void }; 
    presence: { unsubscribe: () => void }; 
    read: { unsubscribe: () => void } 
  }> = new Map();

  // Event handlers
  private onMessageHandlers: ((message: WebSocketMessage) => void)[] = [];
  private onTypingHandlers: ((indicator: TypingIndicator) => void)[] = [];
  private onPresenceHandlers: ((presence: PresenceUpdate) => void)[] = [];
  private onReadReceiptHandlers: ((receipt: ReadReceipt) => void)[] = [];
  private onErrorHandlers: ((error: WebSocketError) => void)[] = [];
  private onConnectionChangeHandlers: ((connected: boolean) => void)[] = [];

  constructor() {
    if (typeof window !== 'undefined') {
      this.initializeClient();
    }
  }

  private initializeClient() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/chat`),
      connectHeaders: {
        Authorization: `Bearer ${this.getAuthToken()}`,
      },
      debug: () => {
        // Debug logging disabled for production
      },
      onConnect: () => {
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.notifyConnectionChange(true);
      },
      onStompError: (frame) => {
        this.notifyError({ error: frame.headers.message || 'WebSocket error', timestamp: Date.now() });
      },
      onWebSocketClose: () => {
        this.isConnected = false;
        this.notifyConnectionChange(false);
        this.handleReconnection();
      },
      onWebSocketError: () => {
        this.notifyError({ error: 'Connection error', timestamp: Date.now() });
      },
    });
  }

  private getAuthToken(): string {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('auth_token') || '';
    }
    return '';
  }

  private handleReconnection() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      // Attempting to reconnect...
      
      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);
    } else {
      // Max reconnection attempts reached
    }
  }

  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.client) {
        reject(new Error('WebSocket client not initialized'));
        return;
      }

      if (this.isConnected) {
        resolve();
        return;
      }

      // Update auth token before connecting
      this.client.connectHeaders = {
        Authorization: `Bearer ${this.getAuthToken()}`,
      };

      this.client.activate();
      
      // Wait for connection
      const checkConnection = () => {
        if (this.isConnected) {
          resolve();
        } else {
          setTimeout(checkConnection, 100);
        }
      };
      checkConnection();
    });
  }

  public disconnect() {
    if (this.client && this.isConnected) {
      this.client.deactivate();
      this.isConnected = false;
      this.subscriptions.clear();
    }
  }

  public subscribeToChat(sessionId: number): void {
    if (!this.client || !this.isConnected) {
      return;
    }

    const sessionKey = `chat-${sessionId}`;

    // Subscribe to messages
    const messageSubscription = this.client.subscribe(
      `/topic/chat.${sessionId}.messages`,
      (message) => {
        try {
          const messageData: WebSocketMessage = JSON.parse(message.body);
          this.notifyMessage(messageData);
        } catch (error) {
          console.error('Error parsing message:', error);
        }
      }
    );

    // Subscribe to typing indicators
    const typingSubscription = this.client.subscribe(
      `/topic/chat.${sessionId}.typing`,
      (message) => {
        try {
          const typingData: TypingIndicator = JSON.parse(message.body);
          this.notifyTyping(typingData);
        } catch (error) {
          console.error('Error parsing typing indicator:', error);
        }
      }
    );

    // Subscribe to presence updates
    const presenceSubscription = this.client.subscribe(
      `/topic/chat.${sessionId}.presence`,
      (message) => {
        try {
          const presenceData: PresenceUpdate = JSON.parse(message.body);
          this.notifyPresence(presenceData);
        } catch (error) {
          console.error('Error parsing presence update:', error);
        }
      }
    );

    // Subscribe to read receipts
    const readSubscription = this.client.subscribe(
      `/topic/chat.${sessionId}.read`,
      (message) => {
        try {
          const readData: ReadReceipt = JSON.parse(message.body);
          this.notifyReadReceipt(readData);
        } catch (error) {
          console.error('Error parsing read receipt:', error);
        }
      }
    );

    // Store subscriptions
    this.subscriptions.set(sessionKey, {
      messages: messageSubscription,
      typing: typingSubscription,
      presence: presenceSubscription,
      read: readSubscription,
    });

    // Send join message
    this.sendJoinMessage(sessionId);
  }

  public unsubscribeFromChat(sessionId: number): void {
    const sessionKey = `chat-${sessionId}`;
    const subscriptions = this.subscriptions.get(sessionKey);

    if (subscriptions) {
      subscriptions.messages.unsubscribe();
      subscriptions.typing.unsubscribe();
      subscriptions.presence.unsubscribe();
      subscriptions.read.unsubscribe();
      this.subscriptions.delete(sessionKey);
    }

    // Send leave message
    this.sendLeaveMessage(sessionId);
  }

  public sendMessage(sessionId: number, content: string): void {
    if (!this.client || !this.isConnected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/chat/${sessionId}/message`,
      body: JSON.stringify({ content }),
    });
  }

  public sendTypingIndicator(sessionId: number, isTyping: boolean): void {
    if (!this.client || !this.isConnected) {
      return;
    }

    this.client.publish({
      destination: `/app/chat/${sessionId}/typing`,
      body: JSON.stringify({ isTyping }),
    });
  }

  public sendReadReceipt(sessionId: number, messageId: number): void {
    if (!this.client || !this.isConnected) {
      return;
    }

    this.client.publish({
      destination: `/app/chat/${sessionId}/read`,
      body: JSON.stringify({ messageId }),
    });
  }

  private sendJoinMessage(sessionId: number): void {
    if (!this.client || !this.isConnected) {
      return;
    }

    this.client.publish({
      destination: `/app/chat/${sessionId}/join`,
      body: JSON.stringify({}),
    });
  }

  private sendLeaveMessage(sessionId: number): void {
    if (!this.client || !this.isConnected) {
      return;
    }

    this.client.publish({
      destination: `/app/chat/${sessionId}/leave`,
      body: JSON.stringify({}),
    });
  }

  // Event handler registration
  public onMessage(handler: (message: WebSocketMessage) => void): void {
    this.onMessageHandlers.push(handler);
  }

  public onTyping(handler: (indicator: TypingIndicator) => void): void {
    this.onTypingHandlers.push(handler);
  }

  public onPresence(handler: (presence: PresenceUpdate) => void): void {
    this.onPresenceHandlers.push(handler);
  }

  public onReadReceipt(handler: (receipt: ReadReceipt) => void): void {
    this.onReadReceiptHandlers.push(handler);
  }

  public onError(handler: (error: WebSocketError) => void): void {
    this.onErrorHandlers.push(handler);
  }

  public onConnectionChange(handler: (connected: boolean) => void): void {
    this.onConnectionChangeHandlers.push(handler);
  }

  // Event notification methods
  private notifyMessage(message: WebSocketMessage): void {
    this.onMessageHandlers.forEach(handler => handler(message));
  }

  private notifyTyping(indicator: TypingIndicator): void {
    this.onTypingHandlers.forEach(handler => handler(indicator));
  }

  private notifyPresence(presence: PresenceUpdate): void {
    this.onPresenceHandlers.forEach(handler => handler(presence));
  }

  private notifyReadReceipt(receipt: ReadReceipt): void {
    this.onReadReceiptHandlers.forEach(handler => handler(receipt));
  }

  private notifyError(error: WebSocketError): void {
    this.onErrorHandlers.forEach(handler => handler(error));
  }

  private notifyConnectionChange(connected: boolean): void {
    this.onConnectionChangeHandlers.forEach(handler => handler(connected));
  }

  public getConnectionStatus(): boolean {
    return this.isConnected;
  }
}

// Export singleton instance
export const webSocketService = new WebSocketService();