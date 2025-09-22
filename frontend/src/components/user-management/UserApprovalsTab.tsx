'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { CheckCircle, XCircle, Clock, UserCheck, Users, Search, ChevronLeft, ChevronRight } from 'lucide-react';
import ApprovalModal from '@/components/ui/ApprovalModal';
import { userApprovalApi } from '@/lib/api';
import {
  ApprovalStatistics,
  UserApprovalDto,
  ApprovalStatus,
  getTranslatedApprovalStatusName
} from '@/types/auth';

interface UserApprovalsTabProps {
  isActive: boolean;
}

export default function UserApprovalsTab({ isActive }: UserApprovalsTabProps) {
  const { token } = useAuth();
  const { t } = useLanguage();
  const [statistics, setStatistics] = useState<ApprovalStatistics | null>(null);
  const [users, setUsers] = useState<UserApprovalDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [_error, setError] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<ApprovalStatus>(ApprovalStatus.PENDING);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [pageSize, setPageSize] = useState(20);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [modalState, setModalState] = useState<{
    isOpen: boolean;
    type: 'approve' | 'reject' | 'reset' | 'enable' | 'disable';
    userPhone: string;
  }>({
    isOpen: false,
    type: 'approve',
    userPhone: ''
  });

  const fetchData = useCallback(async (status: ApprovalStatus, page: number, size: number) => {
    if (!token || !isActive) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const [statsData, usersData] = await Promise.all([
        userApprovalApi.getStatistics(),
        userApprovalApi.getUserApprovals(status, page, size)
      ]);

      setStatistics(statsData);
      setUsers(usersData.items);
      setTotalPages(usersData.totalPages);
      setTotalUsers(usersData.total);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, [token, isActive]);

  useEffect(() => {
    if (isActive) {
      setCurrentPage(1);
      fetchData(selectedStatus, 1, pageSize);
    }
  }, [isActive, selectedStatus, pageSize, fetchData]);

  useEffect(() => {
    if (isActive && currentPage > 1) {
      fetchData(selectedStatus, currentPage, pageSize);
    }
  }, [currentPage, isActive, selectedStatus, pageSize, fetchData]);

  const handleStatusChange = (status: ApprovalStatus) => {
    setSelectedStatus(status);
  };

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

  const openModal = (type: 'approve' | 'reject' | 'reset' | 'enable' | 'disable', userPhone: string) => {
    setModalState({
      isOpen: true,
      type,
      userPhone
    });
  };

  const closeModal = () => {
    setModalState({
      isOpen: false,
      type: 'approve',
      userPhone: ''
    });
  };

  const handleModalConfirm = async (reason?: string) => {
    const { type, userPhone } = modalState;
    setActionLoading(userPhone);

    try {
      switch (type) {
        case 'approve':
          await userApprovalApi.approveUser(userPhone, reason);
          break;
        case 'reject':
          await userApprovalApi.rejectUser(userPhone, reason || 'No reason provided');
          break;
        case 'reset':
          await userApprovalApi.resetUser(userPhone, reason);
          break;
        case 'enable':
          await userApprovalApi.enableUser(userPhone, reason);
          break;
        case 'disable':
          await userApprovalApi.disableUser(userPhone, reason || 'No reason provided');
          break;
      }

      await fetchData(selectedStatus, currentPage, pageSize);
      closeModal();
    } catch (error) {
      console.error(`Failed to ${type} user:`, error);
      alert(`${t('approvals.approveFailed')}. ${t('customers.tryAgain')}.`);
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusBadgeColor = (status: ApprovalStatus) => {
    switch (status) {
      case ApprovalStatus.PENDING:
        return 'bg-yellow-100 text-yellow-800';
      case ApprovalStatus.APPROVED:
        return 'bg-green-100 text-green-800';
      case ApprovalStatus.REJECTED:
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: ApprovalStatus) => {
    switch (status) {
      case ApprovalStatus.PENDING:
        return <Clock size={16} />;
      case ApprovalStatus.APPROVED:
        return <CheckCircle size={16} />;
      case ApprovalStatus.REJECTED:
        return <XCircle size={16} />;
      default:
        return <Clock size={16} />;
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
      {/* Statistics Cards */}
      {statistics && (
        <div className="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
            <dt className="text-sm font-medium text-gray-500 truncate">{t('approvals.pending')}</dt>
            <dd className="mt-1 text-3xl font-semibold text-yellow-600">{statistics.pendingCount}</dd>
          </div>
          <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
            <dt className="text-sm font-medium text-gray-500 truncate">{t('approvals.approved')}</dt>
            <dd className="mt-1 text-3xl font-semibold text-green-600">{statistics.approvedCount}</dd>
          </div>
          <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
            <dt className="text-sm font-medium text-gray-500 truncate">{t('approvals.rejected')}</dt>
            <dd className="mt-1 text-3xl font-semibold text-red-600">{statistics.rejectedCount}</dd>
          </div>
          <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
            <dt className="text-sm font-medium text-gray-500 truncate">{t('dashboard.metrics.conversionRate')}</dt>
            <dd className="mt-1 text-3xl font-semibold text-indigo-600">
              {statistics.approvalRate.toFixed(1)}%
            </dd>
          </div>
        </div>
      )}

      {/* Status Filters and Search */}
      <div className="mt-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="flex gap-2">
          {Object.values(ApprovalStatus).map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => handleStatusChange(status)}
              className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md ${
                selectedStatus === status
                  ? 'bg-indigo-100 text-indigo-700'
                  : 'bg-white text-gray-500 hover:text-gray-700'
              } border border-gray-300`}
            >
              {getStatusIcon(status)}
              <span className="ml-2">{getTranslatedApprovalStatusName(status, t)}</span>
              {statistics && (
                <span className="ml-2 bg-gray-200 text-gray-800 text-xs px-2 py-1 rounded-full">
                  {status === ApprovalStatus.PENDING
                    ? statistics.pendingCount
                    : status === ApprovalStatus.APPROVED
                    ? statistics.approvedCount
                    : statistics.rejectedCount}
                </span>
              )}
            </button>
          ))}
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
                {t('approvals.status')}
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
                <td colSpan={6} className="px-6 py-12 text-center">
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
                        {user.rejectionReason && (
                          <div className="text-sm text-red-600">{t('approvals.rejectionReason')}: {user.rejectionReason}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusBadgeColor(user.approvalStatus)}`}>
                      {getStatusIcon(user.approvalStatus)}
                      <span className="ml-1">{getTranslatedApprovalStatusName(user.approvalStatus, t)}</span>
                    </span>
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
                      {user.approvalStatus === ApprovalStatus.PENDING && (
                        <>
                          <button
                            type="button"
                            onClick={() => openModal('approve', user.phone)}
                            disabled={actionLoading === user.phone}
                            className="text-green-600 hover:text-green-900 disabled:opacity-50"
                            title={t('approvals.approveUser')}
                          >
                            <CheckCircle size={18} />
                          </button>
                          <button
                            type="button"
                            onClick={() => openModal('reject', user.phone)}
                            disabled={actionLoading === user.phone}
                            className="text-red-600 hover:text-red-900 disabled:opacity-50"
                            title={t('approvals.rejectUser')}
                          >
                            <XCircle size={18} />
                          </button>
                        </>
                      )}
                      {user.approvalStatus === ApprovalStatus.APPROVED && (
                        <>
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
                        </>
                      )}
                      {user.approvalStatus !== ApprovalStatus.PENDING && (
                        <button
                          type="button"
                          onClick={() => openModal('reset', user.phone)}
                          disabled={actionLoading === user.phone}
                          className="text-gray-600 hover:text-gray-900 disabled:opacity-50"
                          title={t('approvals.resetToPending')}
                        >
                          <Clock size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="text-center py-12">
                  <Users className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-2 text-sm font-medium text-gray-900">{t('approvals.noUsersFound')}</h3>
                  <p className="mt-1 text-sm text-gray-500">
                    {searchTerm
                      ? `${t('approvals.noUsersFoundMatching')} "${searchTerm}"`
                      : `${t('approvals.noUsersWithStatus')} "${getTranslatedApprovalStatusName(selectedStatus, t)}"`
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