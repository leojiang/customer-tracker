import { chatApi } from './chatApi';
import { ChatMessage } from '@/types/chat';

class MessagePollingService {
  private pollingIntervals: Map<number, NodeJS.Timeout> = new Map();
  private messageHandlers: Map<number, (messages: ChatMessage[]) => void> = new Map();
  private lastMessageIds: Map<number, number> = new Map();
  private isPollingEnabled: boolean = true;

  /**
   * Enable or disable polling globally
   */
  public setPollingEnabled(enabled: boolean): void {
    this.isPollingEnabled = enabled;
    
    if (!enabled) {
      // Stop all polling when disabled
      this.stopAllPolling();
    }
  }

  /**
   * Get current polling status
   */
  public isPollingOn(): boolean {
    return this.isPollingEnabled;
  }

  /**
   * Start polling for new messages in a chat session
   */
  public startPolling(sessionId: number, onNewMessages: (messages: ChatMessage[]) => void): void {
    if (!this.isPollingEnabled) {
      return;
    }
    // Stop existing polling for this session if any
    this.stopPolling(sessionId);

    // Store the handler
    this.messageHandlers.set(sessionId, onNewMessages);

    // Start polling every second
    const interval = setInterval(async () => {
      try {
        await this.pollForNewMessages(sessionId);
      } catch (error) {
        console.error('Error polling for messages:', error);
      }
    }, 1000);

    this.pollingIntervals.set(sessionId, interval);
  }

  /**
   * Stop polling for a specific session
   */
  public stopPolling(sessionId: number): void {
    const interval = this.pollingIntervals.get(sessionId);
    if (interval) {
      clearInterval(interval);
      this.pollingIntervals.delete(sessionId);
    }
    this.messageHandlers.delete(sessionId);
    this.lastMessageIds.delete(sessionId);
  }

  /**
   * Stop all polling
   */
  public stopAllPolling(): void {
    this.pollingIntervals.forEach((interval) => {
      clearInterval(interval);
    });
    this.pollingIntervals.clear();
    this.messageHandlers.clear();
    this.lastMessageIds.clear();
  }

  /**
   * Poll for new messages in a session
   */
  private async pollForNewMessages(sessionId: number): Promise<void> {
    try {
      const messages = await chatApi.getAllMessages(sessionId);
      const handler = this.messageHandlers.get(sessionId);
      
      if (!handler) {
        return;
      }

      const lastMessageId = this.lastMessageIds.get(sessionId);
      
      if (lastMessageId) {
        // Only get messages newer than the last one we've seen
        const newMessages = messages.filter(msg => msg.id > lastMessageId);
        if (newMessages.length > 0) {
          handler(newMessages);
        }
      } else {
        // First time polling - get all messages
        handler(messages);
      }

      // Update the last message ID
      if (messages.length > 0) {
        const latestMessage = messages[messages.length - 1];
        if (latestMessage) {
          this.lastMessageIds.set(sessionId, latestMessage.id);
        }
      }
    } catch (error) {
      console.error('Failed to poll messages for session', sessionId, error);
    }
  }
}

// Export singleton instance
export const messagePollingService = new MessagePollingService();