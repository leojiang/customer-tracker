'use client';

import { useState, useEffect, useRef } from 'react';
import { ChatMessage } from '@/types/chat';
import { chatApi } from '@/services/chatApi';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { useWebSocket } from '@/contexts/WebSocketContext';

interface MessageListProps {
  sessionId: number;
  onMessagesLoaded?: () => void;
}

export default function MessageList({ sessionId, onMessagesLoaded }: MessageListProps) {
  const { t } = useLanguage();
  const { user } = useAuth();
  const { messages: webSocketMessages } = useWebSocket();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchMessages = async () => {
      try {
        setLoading(true);
        setError(null);
        const messagesData = await chatApi.getAllMessages(sessionId);
        setMessages(messagesData);
        
        // Mark messages as read
        await chatApi.markMessagesAsRead(sessionId);
        
        onMessagesLoaded?.();
      } catch (error) {
        console.error('Failed to fetch messages:', error);
        setError('Failed to load messages');
      } finally {
        setLoading(false);
      }
    };

    if (sessionId) {
      fetchMessages();
    }
  }, [sessionId, onMessagesLoaded]);

  useEffect(() => {
    // Scroll to bottom when messages change
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Merge WebSocket messages with fetched messages
  useEffect(() => {
    if (webSocketMessages.length > 0) {
      setMessages(prevMessages => {
        const allMessages = [...prevMessages];
        
        webSocketMessages.forEach(wsMessage => {
          // Check if message already exists
          const exists = allMessages.some(msg => msg.id === wsMessage.id);
          if (!exists) {
            allMessages.push(wsMessage);
          }
        });
        
        // Sort by timestamp
        return allMessages.sort((a, b) => 
          new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
        );
      });
    }
  }, [webSocketMessages]);

  const formatMessageTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const isOwnMessage = (message: ChatMessage) => {
    return message.senderPhone === user?.phone;
  };

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-sm text-surface-500">{t('chat.loadingMessages')}</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-4">
        <div className="text-sm text-red-500 mb-2">{t('chat.error')}</div>
        <button 
          onClick={() => window.location.reload()}
          className="text-sm text-primary-600 hover:text-primary-700"
        >
          {t('chat.retry')}
        </button>
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-4">
        <div className="text-sm text-surface-500">{t('chat.noMessages')}</div>
        <div className="text-xs text-surface-400 mt-1">{t('chat.startConversation')}</div>
      </div>
    );
  }

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    // Prevent scroll event from bubbling up to parent elements
    e.stopPropagation();
  };

  return (
    <div className="h-full overflow-y-auto p-4 space-y-3" onScroll={handleScroll}>
      {messages.map((message) => {
        const isOwn = isOwnMessage(message);
        
        return (
          <div
            key={message.id}
            className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[70%] px-3 py-2 rounded-lg ${
                isOwn
                  ? 'bg-primary-500 text-white'
                  : 'bg-surface-100 text-surface-900'
              }`}
            >
              <div className="text-sm">{message.messageContent}</div>
              <div
                className={`text-xs mt-1 ${
                  isOwn ? 'text-primary-100' : 'text-surface-500'
                }`}
              >
                {formatMessageTime(message.sentAt)}
              </div>
            </div>
          </div>
        );
      })}
      <div ref={messagesEndRef} />
    </div>
  );
}