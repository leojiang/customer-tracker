'use client';

import { useState, useEffect } from 'react';
import { ChatSessionWithOtherParticipant } from '@/types/chat';
import { chatApi } from '@/services/chatApi';
import { useLanguage } from '@/contexts/LanguageContext';
import { MessageCircle, Clock, Plus } from 'lucide-react';

interface ChatSessionListProps {
  selectedSessionId?: number;
  onSessionSelect: (sessionId: number) => void;
  currentUserPhone: string;
  onStartNewChat?: () => void;
}

export default function ChatSessionList({ 
  selectedSessionId, 
  onSessionSelect, 
  currentUserPhone,
  onStartNewChat
}: ChatSessionListProps) {
  const { t } = useLanguage();
  const [sessions, setSessions] = useState<ChatSessionWithOtherParticipant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSessions = async () => {
      try {
        setLoading(true);
        setError(null);
        const sessionsData = await chatApi.getAllChatSessions();
        
        // Fetch other participant info for each session
        const sessionsWithParticipants = await Promise.all(
          sessionsData.map(async (session) => {
            try {
              const otherParticipant = await chatApi.getOtherParticipant(session.id);
              return {
                ...session,
                otherParticipant
              };
            } catch (error) {
              console.error('Failed to fetch participant info:', error);
              return session;
            }
          })
        );
        
        setSessions(sessionsWithParticipants);
      } catch (error) {
        console.error('Failed to fetch chat sessions:', error);
        setError('Failed to load chat sessions');
      } finally {
        setLoading(false);
      }
    };

    fetchSessions();
  }, []);

  const formatTime = (dateString?: string) => {
    if (!dateString) {return '';}
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffInHours < 168) { // 7 days
      return date.toLocaleDateString([], { weekday: 'short' });
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    }
  };

  const getOtherParticipantPhone = (session: ChatSessionWithOtherParticipant) => {
    return session.participant1Phone === currentUserPhone 
      ? session.participant2Phone 
      : session.participant1Phone;
  };

  if (loading) {
    return (
      <div className="flex flex-col h-full">
        <div className="p-3 border-b border-surface-200">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-surface-900">{t('chat.sessions')}</h3>
            {onStartNewChat && (
              <button
                onClick={onStartNewChat}
                className="p-1 hover:bg-surface-100 rounded transition-colors"
                title={t('chat.startNewChat')}
              >
                <Plus size={16} className="text-surface-600" />
              </button>
            )}
          </div>
        </div>
        <div className="flex-1 flex items-center justify-center">
          <div className="text-sm text-surface-500">{t('chat.loadingSessions')}</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col h-full">
        <div className="p-3 border-b border-surface-200">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-surface-900">{t('chat.sessions')}</h3>
            {onStartNewChat && (
              <button
                onClick={onStartNewChat}
                className="p-1 hover:bg-surface-100 rounded transition-colors"
                title={t('chat.startNewChat')}
              >
                <Plus size={16} className="text-surface-600" />
              </button>
            )}
          </div>
        </div>
        <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
          <div className="text-sm text-red-500 mb-2">{t('chat.error')}</div>
          <button 
            onClick={() => window.location.reload()}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            {t('chat.retry')}
          </button>
        </div>
      </div>
    );
  }

  if (sessions.length === 0) {
    return (
      <div className="flex flex-col h-full">
        <div className="p-3 border-b border-surface-200">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-surface-900">{t('chat.sessions')}</h3>
            {onStartNewChat && (
              <button
                onClick={onStartNewChat}
                className="p-1 hover:bg-surface-100 rounded transition-colors"
                title={t('chat.startNewChat')}
              >
                <Plus size={16} className="text-surface-600" />
              </button>
            )}
          </div>
        </div>
        <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
          <MessageCircle size={32} className="text-surface-300 mb-2" />
          <div className="text-sm text-surface-500">{t('chat.noSessions')}</div>
          <div className="text-xs text-surface-400 mt-1">{t('chat.startConversation')}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b border-surface-200">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium text-surface-900">{t('chat.sessions')}</h3>
          {onStartNewChat && (
            <button
              onClick={onStartNewChat}
              className="p-1 hover:bg-surface-100 rounded transition-colors"
              title={t('chat.startNewChat')}
            >
              <Plus size={16} className="text-surface-600" />
            </button>
          )}
        </div>
      </div>
      <div className="flex-1 overflow-y-auto" onScroll={(e) => e.stopPropagation()}>
        {sessions.map((session) => {
          const otherParticipantPhone = getOtherParticipantPhone(session);
          const isSelected = selectedSessionId === session.id;
          
          return (
            <button
              key={session.id}
              onClick={() => onSessionSelect(session.id)}
              className={`w-full p-3 text-left border-b border-surface-100 hover:bg-surface-50 transition-colors ${
                isSelected ? 'bg-primary-50 border-primary-200' : ''
              }`}
            >
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-sm font-medium text-primary-700">
                    {otherParticipantPhone.charAt(0)}
                  </span>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <div className="text-sm font-medium text-surface-900 truncate">
                      {otherParticipantPhone}
                    </div>
                    {session.lastMessageAt && (
                      <div className="flex items-center gap-1 text-xs text-surface-500">
                        <Clock size={12} />
                        {formatTime(session.lastMessageAt)}
                      </div>
                    )}
                  </div>
                  {session.lastMessagePreview && (
                    <div className="text-xs text-surface-600 truncate">
                      {session.lastMessagePreview}
                    </div>
                  )}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}