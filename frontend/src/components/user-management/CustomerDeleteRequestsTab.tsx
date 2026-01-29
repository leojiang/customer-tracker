'use client';

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useUserManagementRefresh } from '@/contexts/UserManagementRefreshContext';
import {
  CheckCircle,
  XCircle,
  Clock,
  User,
  Search,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
} from 'lucide-react';
import { customerDeleteRequestApi } from '@/lib/api';
import {
  CustomerDeleteRequest,
  DeleteRequestStatus,
  DeleteRequestStatistics,
} from '@/types/auth';
import RejectReasonModal from '@/components/ui/RejectReasonModal';
import ApproveReasonModal from '@/components/ui/ApproveReasonModal';

interface CustomerDeleteRequestsTabProps {
  isActive: boolean;
}

export default function CustomerDeleteRequestsTab({
  isActive,
}: CustomerDeleteRequestsTabProps) {
  const { token } = useAuth();
  const { t } = useLanguage();
  const { registerDeleteRequestsRefresh } = useUserManagementRefresh();
  const [statistics, setStatistics] = useState<DeleteRequestStatistics | null>(null);
  const [requests, setRequests] = useState<CustomerDeleteRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [_error, setError] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<DeleteRequestStatus>(DeleteRequestStatus.PENDING);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalRequests, setTotalRequests] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [pageSize, setPageSize] = useState(20);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [rejectModalState, setRejectModalState] = useState<{
    isOpen: boolean;
    requestId: string;
  }>({
    isOpen: false,
    requestId: '',
  });
  const [approveModalState, setApproveModalState] = useState<{
    isOpen: boolean;
    requestId: string;
  }>({
    isOpen: false,
    requestId: '',
  });

  const fetchRequests = useCallback(
    async (page: number, size: number) => {
      if (!token || !isActive) {
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const [statsData, requestsData] = await Promise.all([
          customerDeleteRequestApi.getStatistics(token),
          customerDeleteRequestApi.getAllDeleteRequests(
            token,
            selectedStatus,
            page - 1, // Backend uses 0-based paging
            size,
            'desc'
          )
        ]);

        setStatistics(statsData);
        setRequests(requestsData.items);
        setTotalPages(requestsData.totalPages);
        setTotalRequests(requestsData.total);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    },
    [token, isActive, selectedStatus]
  );

  // Create refresh handler
  const handleRefresh = useCallback(async () => {
    await fetchRequests(currentPage, pageSize);
  }, [fetchRequests, currentPage, pageSize]);

  // Register refresh handler with context
  useEffect(() => {
    if (isActive) {
      registerDeleteRequestsRefresh(handleRefresh);
    }
  }, [isActive, registerDeleteRequestsRefresh, handleRefresh]);

  useEffect(() => {
    if (isActive) {
      setCurrentPage(1);
      fetchRequests(1, pageSize);
    }
  }, [isActive, pageSize, fetchRequests, selectedStatus]);

  useEffect(() => {
    if (isActive) {
      fetchRequests(currentPage, pageSize);
    }
  }, [currentPage, isActive, pageSize, fetchRequests]);

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

    for (
      let i = Math.max(2, currentPage - delta);
      i <= Math.min(totalPages - 1, currentPage + delta);
      i++
    ) {
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

  const openRejectModal = (requestId: string) => {
    setRejectModalState({
      isOpen: true,
      requestId,
    });
  };

  const closeRejectModal = () => {
    setRejectModalState({
      isOpen: false,
      requestId: '',
    });
  };

  const openApproveModal = (requestId: string) => {
    setApproveModalState({
      isOpen: true,
      requestId,
    });
  };

  const closeApproveModal = () => {
    setApproveModalState({
      isOpen: false,
      requestId: '',
    });
  };

  const handleApprove = async (reason?: string) => {
    if (!token) {
      return;
    }

    const { requestId } = approveModalState;
    setActionLoading(requestId);

    try {
      await customerDeleteRequestApi.approveDeleteRequest(
        token,
        requestId,
        reason ?? undefined
      );
      await fetchRequests(currentPage, pageSize);
      closeApproveModal();
    } catch (error) {
      console.error('Failed to approve delete request:', error);
      throw error; // Re-throw to let modal handle the error display
    } finally {
      setActionLoading(null);
    }
  };

  const handleRejectConfirm = async (reason: string) => {
    if (!token) {
      return;
    }

    const { requestId } = rejectModalState;
    setActionLoading(requestId);

    try {
      await customerDeleteRequestApi.rejectDeleteRequest(token, requestId, {
        rejectionReason: reason,
      });
      await fetchRequests(currentPage, pageSize);
      closeRejectModal();
    } catch (error) {
      console.error('Failed to reject delete request:', error);
      throw error; // Re-throw to let modal handle the error display
    } finally {
      setActionLoading(null);
    }
  };

  const filteredRequests = requests.filter(
    (request) =>
      request.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      request.customerPhone.toLowerCase().includes(searchTerm.toLowerCase()) ||
      request.requestedBy.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!isActive) {
    return null;
  }

  return (
    <div className="flex flex-col h-full">
      {/* Statistics Cards - Clickable to filter */}
      {statistics && (
        <div className="flex-shrink-0 mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <button
            type="button"
            onClick={() => {
              setSelectedStatus(DeleteRequestStatus.PENDING);
              setCurrentPage(1);
            }}
            className={`overflow-hidden rounded-lg px-4 py-5 shadow sm:p-6 text-left transition-all duration-200 hover:shadow-lg ${
              selectedStatus === DeleteRequestStatus.PENDING
                ? 'ring-2 ring-yellow-500 bg-yellow-50'
                : 'bg-white hover:bg-gray-50'
            }`}
          >
            <dt className="text-sm font-medium text-gray-500 truncate flex items-center gap-2">
              <Clock size={16} />
              {t('deleteRequests.pending')}
            </dt>
            <dd className="mt-1 text-3xl font-semibold text-yellow-600">{statistics.pendingCount}</dd>
          </button>
          <button
            type="button"
            onClick={() => {
              setSelectedStatus(DeleteRequestStatus.APPROVED);
              setCurrentPage(1);
            }}
            className={`overflow-hidden rounded-lg px-4 py-5 shadow sm:p-6 text-left transition-all duration-200 hover:shadow-lg ${
              selectedStatus === DeleteRequestStatus.APPROVED
                ? 'ring-2 ring-green-500 bg-green-50'
                : 'bg-white hover:bg-gray-50'
            }`}
          >
            <dt className="text-sm font-medium text-gray-500 truncate flex items-center gap-2">
              <CheckCircle size={16} />
              {t('deleteRequests.approved')}
            </dt>
            <dd className="mt-1 text-3xl font-semibold text-green-600">{statistics.approvedCount}</dd>
          </button>
          <button
            type="button"
            onClick={() => {
              setSelectedStatus(DeleteRequestStatus.REJECTED);
              setCurrentPage(1);
            }}
            className={`overflow-hidden rounded-lg px-4 py-5 shadow sm:p-6 text-left transition-all duration-200 hover:shadow-lg ${
              selectedStatus === DeleteRequestStatus.REJECTED
                ? 'ring-2 ring-red-500 bg-red-50'
                : 'bg-white hover:bg-gray-50'
            }`}
          >
            <dt className="text-sm font-medium text-gray-500 truncate flex items-center gap-2">
              <XCircle size={16} />
              {t('deleteRequests.rejected')}
            </dt>
            <dd className="mt-1 text-3xl font-semibold text-red-600">{statistics.rejectedCount}</dd>
          </button>
          <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6 opacity-75 cursor-default">
            <dt className="text-sm font-medium text-gray-500 truncate flex items-center gap-2">
              <AlertCircle size={16} />
              {t('dashboard.metrics.conversionRate')}
            </dt>
            <dd className="mt-1 text-3xl font-semibold text-indigo-600">
              {statistics.approvalRate.toFixed(1)}%
            </dd>
          </div>
        </div>
      )}

      {/* Search Bar - Only */}
      <div className="flex-shrink-0">
        <div className="mt-8 flex justify-end">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder={`${t('deleteRequests.searchPlaceholder')}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-64 pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
        </div>
      </div>

      {/* Requests Table */}
      <div className="flex-1 mt-8 min-h-0 overflow-hidden flex flex-col">
        <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg transition-opacity duration-200 flex flex-col h-full">
          <div className="overflow-y-auto flex-1">
            <table className="min-w-full divide-y divide-gray-300">
              <thead className="bg-gray-50 sticky top-0 z-10">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('customers.name')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('customers.phone')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('deleteRequests.requestedBy')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('deleteRequests.reason')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('deleteRequests.requestedAt')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('approvals.actions')}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {loading ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-6 py-12 text-center"
                    >
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto"></div>
                      <p className="mt-2 text-gray-600">
                        {t('deleteRequests.loadingRequests')}
                      </p>
                    </td>
                  </tr>
                ) : filteredRequests.length > 0 ? (
                  filteredRequests.map((request) => (
                    <tr
                      key={request.id}
                      className="hover:bg-gray-50"
                    >
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="flex-shrink-0 h-10 w-10">
                            <div className="h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center">
                              <User className="h-6 w-6 text-indigo-600" />
                            </div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">
                              {request.customerName}
                            </div>
                            {request.rejectionReason && (
                              <div className="text-sm text-red-600">
                                {t('deleteRequests.rejectionReason')}: {request.rejectionReason}
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {request.customerPhone}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {request.requestedBy}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900 max-w-xs truncate">
                        {request.reason}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(request.createdAt).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => openApproveModal(request.id)}
                            disabled={
                              actionLoading === request.id ||
                              request.requestStatus !== 'PENDING'
                            }
                            className="text-green-600 hover:text-green-900 disabled:opacity-50"
                            title={t('deleteRequests.approve')}
                          >
                            <CheckCircle size={18} />
                          </button>
                          <button
                            type="button"
                            onClick={() => openRejectModal(request.id)}
                            disabled={
                              actionLoading === request.id ||
                              request.requestStatus !== 'PENDING'
                            }
                            className="text-red-600 hover:text-red-900 disabled:opacity-50"
                            title={t('deleteRequests.reject')}
                          >
                            <XCircle size={18} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={6}
                      className="text-center py-12"
                    >
                      <AlertCircle className="mx-auto h-12 w-12 text-gray-400" />
                      <h3 className="mt-2 text-sm font-medium text-gray-900">
                        {t('deleteRequests.noRequestsFound')}
                      </h3>
                      <p className="mt-1 text-sm text-gray-500">
                        {searchTerm
                          ? `${t('deleteRequests.noRequestsMatching')} "${searchTerm}"`
                          : selectedStatus === DeleteRequestStatus.PENDING
                          ? t('deleteRequests.noPendingRequests')
                          : selectedStatus === DeleteRequestStatus.APPROVED
                          ? t('deleteRequests.noApprovedRequests')
                          : t('deleteRequests.noRejectedRequests')}
                      </p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Pagination */}
      <div className="flex-shrink-0 pb-6">
      <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="text-sm text-gray-700">
            {t('approvals.showing')} {Math.min((currentPage - 1) * pageSize + 1, totalRequests)}{' '}
            {t('approvals.to')} {Math.min(currentPage * pageSize, totalRequests)} {t('approvals.of')}{' '}
            {totalRequests} {t('deleteRequests.requests')}
          </div>
          <div className="flex items-center gap-2">
            <label
              htmlFor="pageSize"
              className="text-sm text-gray-700"
            >
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
              <ChevronLeft
                size={16}
                className="-ml-1"
              />
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
                  onClick={() =>
                    typeof pageNum === 'number' ? handlePageChange(pageNum) : undefined
                  }
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
              <ChevronRight
                size={16}
                className="-ml-1"
              />
            </button>
          </div>
        )}
      </div>
      </div>

      {/* Reject Modal */}
      <RejectReasonModal
        isOpen={rejectModalState.isOpen}
        onClose={closeRejectModal}
        onConfirm={handleRejectConfirm}
        loading={actionLoading === rejectModalState.requestId}
      />

      {/* Approve Modal */}
      <ApproveReasonModal
        isOpen={approveModalState.isOpen}
        onClose={closeApproveModal}
        onConfirm={handleApprove}
        loading={actionLoading === approveModalState.requestId}
      />
    </div>
  );
}
