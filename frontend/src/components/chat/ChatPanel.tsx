'use client';

import { useState, useEffect } from 'react';
import { X, MessageCircle } from 'lucide-react';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
// WebSocket context removed - using HTTP polling instead
import { chatApi } from '@/services/chatApi';
import ChatSessionList from './ChatSessionList';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import UserSearch from './UserSearch';

interface ChatPanelProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function ChatPanel({ isOpen, onClose }: ChatPanelProps) {
  const { t } = useLanguage();
  const { user } = useAuth();
  // Remove WebSocket dependencies - using HTTP polling instead
  const [selectedSessionId, setSelectedSessionId] = useState<number | undefined>();
  const [sendingMessage, setSendingMessage] = useState(false);
  const [showUserSearch, setShowUserSearch] = useState(false);
  const [sessionError, setSessionError] = useState<string | null>(null);

  // Lock body scroll when chat panel is open
  useEffect(() => {
    if (isOpen) {
      // Store the current scroll position
      const scrollY = window.scrollY;
      // Lock the body scroll
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
      
      return () => {
        // Restore scroll when component unmounts or closes
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.overflow = '';
        window.scrollTo(0, scrollY);
      };
    }
  }, [isOpen]);

  const handleSessionSelect = (sessionId: number) => {
    setSelectedSessionId(sessionId);
    // No need to subscribe to WebSocket - polling handles updates
  };

  // No WebSocket subscription needed - polling handles updates

  const handleSendMessage = async (messageContent: string) => {
    if (!selectedSessionId || !messageContent.trim()) {
      return;
    }

    try {
      setSendingMessage(true);
      
      // Always use REST API - polling will pick up the new message
      await chatApi.sendMessage(selectedSessionId, messageContent.trim());
    } catch (error) {
      console.error('Failed to send message:', error);
      // You could add a toast notification here
    } finally {
      setSendingMessage(false);
    }
  };

  const handleMessagesLoaded = () => {
    // Messages have been loaded and marked as read
  };

  const handleUserSelect = async (user: { phone: string; role: string }) => {
    try {
      setSessionError(null);
      const session = await chatApi.getOrCreateChatSession(user.phone);
      setSelectedSessionId(session.id);
      setShowUserSearch(false);
    } catch (error) {
      console.error('Failed to create chat session:', error);
      setSessionError('Failed to start chat. The user may not exist or you cannot chat with yourself.');
      // Keep the search open so user can try again
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black bg-opacity-50"
        onClick={onClose}
      />
      
      {/* Chat Panel */}
      <div className="relative bg-white rounded-lg shadow-xl w-full max-w-4xl h-[600px] flex" onScroll={(e) => e.stopPropagation()}>
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-20 p-2 text-surface-500 hover:text-surface-700 hover:bg-surface-100 rounded-lg transition-colors"
        >
          <X size={20} />
        </button>

        {/* Left Sidebar - Chat Sessions */}
        <div className="w-80 border-r border-surface-200 flex flex-col">
          <div className="p-4 border-b border-surface-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MessageCircle size={20} className="text-primary-600" />
                <h2 className="text-lg font-semibold text-surface-900">{t('chat.title')}</h2>
              </div>
              <div className="flex items-center gap-2">
                <div className="flex items-center gap-1 text-blue-600">
                  <MessageCircle size={16} />
                  <span className="text-xs">Polling</span>
                </div>
              </div>
            </div>
          </div>
          {showUserSearch ? (
            <UserSearch
              onUserSelect={handleUserSelect}
              onClose={() => setShowUserSearch(false)}
              sessionError={sessionError}
            />
          ) : (
            <ChatSessionList
              selectedSessionId={selectedSessionId}
              onSessionSelect={handleSessionSelect}
              currentUserPhone={user?.phone || ''}
              onStartNewChat={() => {
                setSessionError(null);
                setShowUserSearch(true);
              }}
            />
          )}
        </div>

        {/* Right Side - Messages */}
        <div className="flex-1 flex flex-col min-h-0">
          {selectedSessionId ? (
            <>
              {/* Messages - with top padding to avoid close button overlap */}
              <div className="flex-1 pt-16 pb-0 min-h-0 overflow-hidden" onScroll={(e) => e.stopPropagation()}>
                <MessageList
                  sessionId={selectedSessionId}
                  onMessagesLoaded={handleMessagesLoaded}
                />
              </div>
              
              {/* Message Input - fixed at bottom */}
              <div className="flex-shrink-0">
                <MessageInput
                  onSendMessage={handleSendMessage}
                  disabled={sendingMessage}
                />
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center pt-16">
              <div className="text-center">
                <MessageCircle size={48} className="text-surface-300 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-surface-900 mb-2">
                  {t('chat.title')}
                </h3>
                <p className="text-surface-500">
                  {t('chat.startConversation')}
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}