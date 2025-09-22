'use client';

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { webSocketService, WebSocketMessage } from '@/services/websocketService';
import { useAuth } from './AuthContext';

interface WebSocketContextType {
  isConnected: boolean;
  currentSessionId: number | null;
  messages: WebSocketMessage[];
  typingUsers: Set<string>;
  onlineUsers: Set<string>;
  connect: () => Promise<void>;
  disconnect: () => void;
  subscribeToChat: (sessionId: number) => void;
  unsubscribeFromChat: (sessionId: number) => void;
  sendMessage: (sessionId: number, content: string) => void;
  sendTypingIndicator: (sessionId: number, isTyping: boolean) => void;
  sendReadReceipt: (sessionId: number, messageId: number) => void;
  addMessage: (message: WebSocketMessage) => void;
  clearMessages: () => void;
  error: string | null;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

interface WebSocketProviderProps {
  children: ReactNode;
}

export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const { user } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(null);
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const [typingUsers, setTypingUsers] = useState<Set<string>>(new Set());
  const [onlineUsers, setOnlineUsers] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Set up event handlers
    webSocketService.onConnectionChange((connected) => {
      setIsConnected(connected);
      if (!connected) {
        setError('Connection lost. Attempting to reconnect...');
      } else {
        setError(null);
      }
    });

    webSocketService.onMessage((message) => {
      setMessages(prev => {
        // Check if message already exists to avoid duplicates
        const exists = prev.some(m => m.id === message.id);
        if (exists) {
          return prev;
        }
        return [...prev, message].sort((a, b) => 
          new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
        );
      });
    });

    webSocketService.onTyping((indicator) => {
      setTypingUsers(prev => {
        const newSet = new Set(prev);
        if (indicator.isTyping) {
          newSet.add(indicator.userPhone);
        } else {
          newSet.delete(indicator.userPhone);
        }
        return newSet;
      });
    });

    webSocketService.onPresence((presence) => {
      setOnlineUsers(prev => {
        const newSet = new Set(prev);
        if (presence.isOnline) {
          newSet.add(presence.userPhone);
        } else {
          newSet.delete(presence.userPhone);
        }
        return newSet;
      });
    });

    webSocketService.onReadReceipt((receipt) => {
      setMessages(prev => 
        prev.map(message => 
          message.id === receipt.messageId 
            ? { ...message, isRead: true, readAt: receipt.timestamp }
            : message
        )
      );
    });

    webSocketService.onError((error) => {
      setError(error.error);
    });

    // Auto-connect when user is available
    if (user) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [user]);

  const connect = async () => {
    try {
      await webSocketService.connect();
      setError(null);
    } catch (err) {
      setError('Failed to connect to chat server');
      console.error('WebSocket connection failed:', err);
    }
  };

  const disconnect = () => {
    webSocketService.disconnect();
    setCurrentSessionId(null);
    setMessages([]);
    setTypingUsers(new Set());
    setOnlineUsers(new Set());
  };

  const subscribeToChat = (sessionId: number) => {
    if (currentSessionId !== sessionId) {
      // Unsubscribe from previous session if any
      if (currentSessionId) {
        webSocketService.unsubscribeFromChat(currentSessionId);
      }
      
      // Subscribe to new session
      webSocketService.subscribeToChat(sessionId);
      setCurrentSessionId(sessionId);
      setMessages([]); // Clear messages when switching sessions
    }
  };

  const unsubscribeFromChat = (sessionId: number) => {
    webSocketService.unsubscribeFromChat(sessionId);
    if (currentSessionId === sessionId) {
      setCurrentSessionId(null);
      setMessages([]);
      setTypingUsers(new Set());
      setOnlineUsers(new Set());
    }
  };

  const sendMessage = (sessionId: number, content: string) => {
    webSocketService.sendMessage(sessionId, content);
  };

  const sendTypingIndicator = (sessionId: number, isTyping: boolean) => {
    webSocketService.sendTypingIndicator(sessionId, isTyping);
  };

  const sendReadReceipt = (sessionId: number, messageId: number) => {
    webSocketService.sendReadReceipt(sessionId, messageId);
  };

  const addMessage = (message: WebSocketMessage) => {
    setMessages(prev => {
      const exists = prev.some(m => m.id === message.id);
      if (exists) {
        return prev;
      }
      return [...prev, message].sort((a, b) => 
        new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
      );
    });
  };

  const clearMessages = () => {
    setMessages([]);
  };

  const value: WebSocketContextType = {
    isConnected,
    currentSessionId,
    messages,
    typingUsers,
    onlineUsers,
    connect,
    disconnect,
    subscribeToChat,
    unsubscribeFromChat,
    sendMessage,
    sendTypingIndicator,
    sendReadReceipt,
    addMessage,
    clearMessages,
    error,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
}