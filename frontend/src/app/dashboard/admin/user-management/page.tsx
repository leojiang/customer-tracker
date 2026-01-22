'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { UserManagementRefreshProvider } from '@/contexts/UserManagementRefreshContext';
import { useRouter } from 'next/navigation';
import AllUsersTab from '@/components/user-management/AllUsersTab';
import UserApprovalsTab from '@/components/user-management/UserApprovalsTab';
import CustomerDeleteRequestsTab from '@/components/user-management/CustomerDeleteRequestsTab';

/**
 * Admin User Management Dashboard Content
 * Allows admins to manage user registrations and account status
 */
function UserManagementPageContent() {
  const { user, token } = useAuth();
  const { t } = useLanguage();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<'all-users' | 'approvals' | 'delete-requests'>('all-users');

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
    <div className="flex flex-col bg-gray-50 h-full">
      <div className="px-4 sm:px-6 lg:px-8">
        {/* Tab Navigation */}
        <div className="mt-8 border-b border-gray-200 flex-shrink-0">
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
            <button
              type="button"
              onClick={() => setActiveTab('delete-requests')}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'delete-requests'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {t('deleteRequests.title')}
            </button>
          </nav>
        </div>
      </div>

      {/* Tab Content - Takes full height */}
      <div className="flex-1 px-4 sm:px-6 lg:px-8 overflow-hidden">
        <AllUsersTab isActive={activeTab === 'all-users'} />
        <UserApprovalsTab isActive={activeTab === 'approvals'} />
        <CustomerDeleteRequestsTab isActive={activeTab === 'delete-requests'} />
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
