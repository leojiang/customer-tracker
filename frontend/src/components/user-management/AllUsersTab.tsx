'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useUserManagementRefresh } from '@/contexts/UserManagementRefreshContext';
import { CheckCircle, XCircle, UserCheck, Users, Search, ChevronLeft, ChevronRight } from 'lucide-react';
import ApprovalModal from '@/components/ui/ApprovalModal';
import { userApprovalApi } from '@/lib/api';
import { UserApprovalDto } from '@/types/auth';

interface AllUsersTabProps {
  isActive: boolean;
}

export default function AllUsersTab({ isActive }: AllUsersTabProps) {
  const { token } = useAuth();
  const { t } = useLanguage();
  const { registerAllUsersRefresh } = useUserManagementRefresh();
  const [users, setUsers] = useState<UserApprovalDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [_error, setError] = useState<string | null>(null);
  const [userStatusTab, setUserStatusTab] = useState<'active' | 'disabled'>('active');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [pageSize, setPageSize] = useState(20);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [modalState, setModalState] = useState<{
    isOpen: boolean;
    type: 'enable' | 'disable';
    userPhone: string;
  }>({
    isOpen: false,
    type: 'enable',
    userPhone: ''
  });

  const fetchUsers = useCallback(async (statusEnabled: boolean, page: number, size: number) => {
    if (!token || !isActive) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const usersData = await userApprovalApi.getApprovedUsersByEnabledStatus(statusEnabled, page, size);
      setUsers(usersData.items);
      setTotalPages(usersData.totalPages);
      setTotalUsers(usersData.total);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, [token, isActive]);

  // Create refresh handler that refreshes current status
  const handleRefresh = useCallback(async () => {
    await fetchUsers(userStatusTab === 'active', currentPage, pageSize);
  }, [fetchUsers, userStatusTab, currentPage, pageSize]);

  // Register refresh handler with context
  useEffect(() => {
    if (isActive) {
      registerAllUsersRefresh(handleRefresh);
    }
  }, [isActive, registerAllUsersRefresh, handleRefresh]);

  useEffect(() => {
    if (isActive) {
      setCurrentPage(1);
      fetchUsers(userStatusTab === 'active', 1, pageSize);
    }
  }, [isActive, userStatusTab, pageSize, fetchUsers]);

  useEffect(() => {
    if (isActive && currentPage > 1) {
      fetchUsers(userStatusTab === 'active', currentPage, pageSize);
    }
  }, [currentPage, isActive, userStatusTab, pageSize, fetchUsers]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
  };

  const getPageNumbers = () => {
    const delta = 2;
    const range: number[] = [];
    const rangeWithDots: (number | string)[] = [];

    for (let i = Math.max(2, currentPage - delta); i <= Math.min(totalPages - 1, currentPage + delta); i++) {
      range.push(i);
    }

    if (currentPage - delta > 2) {
      rangeWithDots.push(1, '...');
    } else {
      rangeWithDots.push(1);
    }

    rangeWithDots.push(...range);

    if (currentPage + delta < totalPages - 1) {
      rangeWithDots.push('...', totalPages);
    } else {
      if (totalPages > 1) {
        rangeWithDots.push(totalPages);
      }
    }

    return rangeWithDots;
  };

  const openModal = (type: 'enable' | 'disable', userPhone: string) => {
    setModalState({
      isOpen: true,
      type,
      userPhone
    });
  };

  const closeModal = () => {
    setModalState({
      isOpen: false,
      type: 'enable',
      userPhone: ''
    });
  };

  const handleModalConfirm = async (reason?: string) => {
    const { type, userPhone } = modalState;
    setActionLoading(userPhone);

    try {
      if (type === 'enable') {
        await userApprovalApi.enableUser(userPhone, reason);
      } else {
        await userApprovalApi.disableUser(userPhone, reason || 'No reason provided');
      }

      await fetchUsers(userStatusTab === 'active', currentPage, pageSize);
      closeModal();
    } catch (error) {
      console.error(`Failed to ${type} user:`, error);
      alert(`${t('approvals.approveFailed')}. ${t('customers.tryAgain')}.`);
    } finally {
      setActionLoading(null);
    }
  };

  const filteredUsers = users.filter(user =>
    user.phone.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!isActive) {
    return null;
  }

  return (
    <div>
      {/* Sub-tab Navigation */}
      <div className="mt-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setUserStatusTab('active')}
            className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md ${
              userStatusTab === 'active'
                ? 'bg-green-100 text-green-700'
                : 'bg-white text-gray-500 hover:text-gray-700'
            } border border-gray-300`}
          >
            <CheckCircle size={16} />
            <span className="ml-2">{t('userManagement.active')}</span>
          </button>
          <button
            type="button"
            onClick={() => setUserStatusTab('disabled')}
            className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md ${
              userStatusTab === 'disabled'
                ? 'bg-red-100 text-red-700'
                : 'bg-white text-gray-500 hover:text-gray-700'
            } border border-gray-300`}
          >
            <XCircle size={16} />
            <span className="ml-2">{t('userManagement.disabled')}</span>
          </button>
        </div>

        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-5 w-5 text-gray-400" />
          </div>
          <input
            type="text"
            placeholder={`${t('approvals.phone')}...`}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-64 pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          />
        </div>
      </div>

      {/* Users Table */}
      <div className="mt-8 overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg transition-opacity duration-200">
        <table className="min-w-full divide-y divide-gray-300">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {t('approvals.phone')}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {t('userManagement.accountStatus')}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {t('approvals.requestDate')}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {t('approvals.daysWaiting')}
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                {t('approvals.actions')}
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto"></div>
                  <p className="mt-2 text-gray-600">{t('approvals.loadingRequests')}</p>
                </td>
              </tr>
            ) : filteredUsers.length > 0 ? (
              filteredUsers.map((user) => (
                <tr key={user.phone} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-10 w-10">
                        <div className="h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center">
                          <UserCheck className="h-6 w-6 text-indigo-600" />
                        </div>
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">{user.phone}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      user.isEnabled
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {user.isEnabled ? (
                        <>
                          <CheckCircle size={12} className="mr-1" />
                          {t('userManagement.enabled')}
                        </>
                      ) : (
                        <>
                          <XCircle size={12} className="mr-1" />
                          {t('userManagement.disabled')}
                        </>
                      )}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.daysWaiting} {t('approvals.days')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex gap-2">
                      {user.isEnabled ? (
                        <button
                          type="button"
                          onClick={() => openModal('disable', user.phone)}
                          disabled={actionLoading === user.phone}
                          className="text-orange-600 hover:text-orange-900 disabled:opacity-50"
                          title={t('userManagement.disableUser')}
                        >
                          <XCircle size={18} />
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => openModal('enable', user.phone)}
                          disabled={actionLoading === user.phone}
                          className="text-green-600 hover:text-green-900 disabled:opacity-50"
                          title={t('userManagement.enableUser')}
                        >
                          <CheckCircle size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5} className="text-center py-12">
                  <Users className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">{t('approvals.noUsersFound')}</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    {searchTerm
                      ? `${t('approvals.noUsersFoundMatching')} "${searchTerm}"`
                      : `${t('approvals.noUsersWithStatus')} "${userStatusTab === 'active' ? t('userManagement.active') : t('userManagement.disabled')}" ${t('approvals.users')}`
                    }
                  </p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="text-sm text-gray-700">
            {t('approvals.showing')} {Math.min((currentPage - 1) * pageSize + 1, totalUsers)} {t('approvals.to')} {Math.min(currentPage * pageSize, totalUsers)} {t('approvals.of')} {totalUsers} {t('approvals.users')}
          </div>
          <div className="flex items-center gap-2">
            <label htmlFor="pageSize" className="text-sm text-gray-700">
              {t('approvals.show')}
            </label>
            <select
              id="pageSize"
              value={pageSize}
              onChange={(e) => handlePageSizeChange(parseInt(e.target.value))}
              className="text-sm border border-gray-300 rounded-md px-2 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
            <span className="text-sm text-gray-700">{t('approvals.perPage')}</span>
          </div>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => handlePageChange(1)}
              disabled={currentPage === 1}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              title={t('approvals.firstPage')}
            >
              <ChevronLeft size={16} />
              <ChevronLeft size={16} className="-ml-1" />
            </button>

            <button
              type="button"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title={t('approvals.previousPage')}
            >
              <ChevronLeft size={16} />
            </button>

            <div className="flex items-center gap-1">
              {getPageNumbers().map((pageNum, index) => (
                <button
                  key={`${pageNum}-${index}`}
                  type="button"
                  onClick={() => typeof pageNum === 'number' ? handlePageChange(pageNum) : undefined}
                  disabled={typeof pageNum !== 'number'}
                  className={`px-3 py-2 text-sm font-medium rounded-md ${
                    pageNum === currentPage
                      ? 'bg-indigo-600 text-white border border-indigo-600'
                      : typeof pageNum === 'number'
                      ? 'text-gray-700 bg-white border border-gray-300 hover:bg-gray-50'
                      : 'text-gray-400 bg-white border border-transparent cursor-default'
                  }`}
                >
                  {pageNum}
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title={t('approvals.nextPage')}
            >
              <ChevronRight size={16} />
            </button>

            <button
              type="button"
              onClick={() => handlePageChange(totalPages)}
              disabled={currentPage === totalPages}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              title={t('approvals.lastPage')}
            >
              <ChevronRight size={16} />
              <ChevronRight size={16} className="-ml-1" />
            </button>
          </div>
        )}
      </div>

      {/* Modal */}
      <ApprovalModal
        isOpen={modalState.isOpen}
        onClose={closeModal}
        onConfirm={handleModalConfirm}
        type={modalState.type}
        userPhone={modalState.userPhone}
        loading={actionLoading === modalState.userPhone}
      />
    </div>
  );
}