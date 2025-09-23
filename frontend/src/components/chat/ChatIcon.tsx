'use client';

import { useState, useEffect } from 'react';
import { MessageCircle } from 'lucide-react';
import { chatApi } from '@/services/chatApi';
import { messagePollingService } from '@/services/messagePollingService';

interface ChatIconProps {
  onClick: () => void;
  className?: string;
}

export default function ChatIcon({ onClick, className = '' }: ChatIconProps) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [isPollingEnabled, setIsPollingEnabled] = useState(messagePollingService.isPollingOn());

  useEffect(() => {
    const fetchUnreadCount = async () => {
      try {
        setLoading(true);
        const count = await chatApi.getUnreadCount();
        setUnreadCount(count);
      } catch (error) {
        console.error('Failed to fetch unread count:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUnreadCount();

    // Only start polling if polling is enabled
    if (isPollingEnabled) {
      // Refresh unread count every 5 seconds
      const interval = setInterval(fetchUnreadCount, 5000);
      return () => clearInterval(interval);
    }
  }, [isPollingEnabled]);

  // Listen for polling status changes
  useEffect(() => {
    const checkPollingStatus = () => {
      const currentStatus = messagePollingService.isPollingOn();
      if (currentStatus !== isPollingEnabled) {
        setIsPollingEnabled(currentStatus);
      }
    };

    // Check polling status every second
    const interval = setInterval(checkPollingStatus, 1000);
    
    return () => clearInterval(interval);
  }, [isPollingEnabled]);

  return (
    <button
      onClick={onClick}
      className={`relative flex items-center justify-center w-10 h-10 rounded-lg bg-surface-100 hover:bg-surface-200 transition-colors ${className}`}
      title="Chat"
    >
      <MessageCircle size={20} className="text-surface-600" />
      {unreadCount > 0 && (
        <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1">
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
      )}
    </button>
  );
}