'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useUserManagementRefresh } from '@/contexts/UserManagementRefreshContext';
import { UserManagementRefreshProvider } from '@/contexts/UserManagementRefreshContext';
import { useRouter } from 'next/navigation';
import { RefreshCw } from 'lucide-react';
import AllUsersTab from '@/components/user-management/AllUsersTab';
import UserApprovalsTab from '@/components/user-management/UserApprovalsTab';

/**
 * Admin User Management Dashboard Content
 * Allows admins to manage user registrations and account status
 */
function UserManagementPageContent() {
  const { user, token } = useAuth();
  const { t } = useLanguage();
  const { refreshAllUsers, refreshUserApprovals, isRefreshing } = useUserManagementRefresh();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<'all-users' | 'approvals'>('all-users');

  useEffect(() => {
    if (!user || !token) {
      router.push('/auth');
      return;
    }

    if (user.role !== 'ADMIN') {
      router.push('/dashboard/sales');
      return;
    }
  }, [user, token, router]);

  const handleRefresh = () => {
    if (activeTab === 'all-users') {
      refreshAllUsers();
    } else if (activeTab === 'approvals') {
      refreshUserApprovals();
    }
  };

  if (!user || !token || user.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">{t('approvals.loadingRequests')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 overflow-y-scroll">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="min-w-0 flex-1">
            <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:text-3xl sm:tracking-tight">
              {t('userManagement.title')}
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              {t('userManagement.subtitle')}
            </p>
          </div>
          <div className="mt-4 flex gap-3 md:ml-4 md:mt-0">
            <button
              type="button"
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw 
                size={16} 
                className={`mr-1 ${isRefreshing ? 'animate-spin' : ''}`} 
              />
              {isRefreshing ? t('nav.refreshing') : t('nav.refresh')}
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="mt-8 border-b border-gray-200">
          <nav className="-mb-px flex space-x-8">
            <button
              type="button"
              onClick={() => setActiveTab('all-users')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'all-users'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {t('userManagement.allUsers')}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('approvals')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'approvals'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {t('userManagement.userApprovals')}
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        <AllUsersTab isActive={activeTab === 'all-users'} />
        <UserApprovalsTab isActive={activeTab === 'approvals'} />
      </div>
    </div>
  );
}

/**
 * Main User Management Page with Refresh Provider
 */
export default function UserManagementPage() {
  return (
    <UserManagementRefreshProvider>
      <UserManagementPageContent />
    </UserManagementRefreshProvider>
  );
}