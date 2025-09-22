'use client';

import { useState, useEffect } from 'react';
import { Search, User, MessageCircle } from 'lucide-react';
import { useLanguage } from '@/contexts/LanguageContext';
import { chatApi } from '@/services/chatApi';

interface User {
  phone: string;
  role: string;
}

interface UserSearchProps {
  onUserSelect: (user: User) => void;
  onClose: () => void;
  sessionError?: string | null;
}

export default function UserSearch({ onUserSelect, onClose, sessionError }: UserSearchProps) {
  const { t } = useLanguage();
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const searchUsers = async () => {
      if (!query.trim()) {
        setUsers([]);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const results = await chatApi.searchUsers(query.trim());
        setUsers(results);
      } catch (error) {
        console.error('Failed to search users:', error);
        setError('Failed to search users');
        setUsers([]);
      } finally {
        setLoading(false);
      }
    };

    const timeoutId = setTimeout(searchUsers, 300); // Debounce search
    return () => clearTimeout(timeoutId);
  }, [query]);

  const handleUserSelect = (user: User) => {
    onUserSelect(user);
    // Don't call onClose() here - let the parent component handle it
    // onClose() will be called by ChatPanel after successful session creation
  };

  return (
    <div className="flex flex-col h-full">
      {/* Search Header */}
      <div className="p-3 border-b border-surface-200">
        <div className="flex items-center gap-2 mb-3">
          <button
            onClick={onClose}
            className="p-1 hover:bg-surface-100 rounded"
          >
            <MessageCircle size={16} className="text-surface-600" />
          </button>
          <h3 className="text-sm font-medium text-surface-900">{t('chat.startConversation')}</h3>
        </div>
        
        {/* Search Input */}
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-surface-400" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('chat.searchUsers')}
            className="w-full pl-9 pr-3 py-2 border border-surface-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
          />
        </div>
      </div>

      {/* Search Results */}
      <div className="flex-1 overflow-y-auto">
        {loading && (
          <div className="flex items-center justify-center p-4">
            <div className="text-sm text-surface-500">{t('common.loading')}</div>
          </div>
        )}

        {error && (
          <div className="flex items-center justify-center p-4">
            <div className="text-sm text-red-500">{error}</div>
          </div>
        )}

        {sessionError && (
          <div className="flex items-center justify-center p-4">
            <div className="text-sm text-red-500">{sessionError}</div>
          </div>
        )}

        {!loading && !error && query.trim() && users.length === 0 && (
          <div className="flex items-center justify-center p-4">
            <div className="text-sm text-surface-500">{t('chat.noUsersFound')}</div>
          </div>
        )}

        {!loading && !error && users.length > 0 && (
          <div className="p-2">
            {users.map((user) => (
              <button
                key={user.phone}
                onClick={() => handleUserSelect(user)}
                className="w-full p-3 text-left hover:bg-surface-50 rounded-lg transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                    <span className="text-sm font-medium text-primary-700">
                      {user.phone.charAt(0)}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium text-surface-900 truncate">
                      {user.phone}
                    </div>
                    <div className="text-xs text-surface-500">
                      {user.role === 'ADMIN' ? t('general.admin') : t('general.sales')}
                    </div>
                  </div>
                  <MessageCircle size={16} className="text-surface-400" />
                </div>
              </button>
            ))}
          </div>
        )}

        {!query.trim() && (
          <div className="flex flex-col items-center justify-center p-8 text-center">
            <User size={32} className="text-surface-300 mb-2" />
            <div className="text-sm text-surface-500 mb-1">{t('chat.searchUsers')}</div>
            <div className="text-xs text-surface-400">{t('chat.searchUsersHint')}</div>
          </div>
        )}
      </div>
    </div>
  );
}