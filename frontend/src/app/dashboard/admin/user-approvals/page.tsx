'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { ArrowLeft, CheckCircle, XCircle, Clock, UserCheck, Users, RefreshCw, Search, Filter } from 'lucide-react';
import ApprovalModal from '@/components/ui/ApprovalModal';
import { userApprovalApi } from '@/lib/api';
import { 
  ApprovalStatistics, 
  UserApprovalDto, 
  ApprovalStatus,
  ApprovalPageResponse 
} from '@/types/auth';

/**
 * Admin User Approvals Dashboard
 * Allows admins to manage pending user registrations
 */
export default function UserApprovalsPage() {
  const { user, token } = useAuth();
  const router = useRouter();
  const [statistics, setStatistics] = useState<ApprovalStatistics | null>(null);
  const [users, setUsers] = useState<UserApprovalDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState<ApprovalStatus>(ApprovalStatus.PENDING);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [modalState, setModalState] = useState<{
    isOpen: boolean;
    type: 'approve' | 'reject' | 'reset';
    userPhone: string;
  }>({
    isOpen: false,
    type: 'approve',
    userPhone: ''
  });

  const fetchData = useCallback(async (isRefresh = false) => {
    if (!token) return;

    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      // Fetch statistics and users in parallel
      const [statsData, usersData] = await Promise.all([
        userApprovalApi.getStatistics(),
        userApprovalApi.getUserApprovals(selectedStatus, currentPage, 20)
      ]);

      setStatistics(statsData);
      setUsers(usersData.items);
      setTotalPages(usersData.totalPages);
      setTotalUsers(usersData.total);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [token, selectedStatus, currentPage]);

  useEffect(() => {
    if (!user || !token) {
      router.push('/auth');
      return;
    }

    if (user.role !== 'ADMIN') {
      router.push('/dashboard/sales');
      return;
    }

    fetchData();
  }, [user, token, router, fetchData]);

  // Reset page when status changes
  useEffect(() => {
    setCurrentPage(1);
  }, [selectedStatus]);

  const handleStatusChange = (status: ApprovalStatus) => {
    setSelectedStatus(status);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const openModal = (type: 'approve' | 'reject' | 'reset', userPhone: string) => {
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
      }
      
      await fetchData(true); // Refresh data
      closeModal();
    } catch (error) {
      console.error(`Failed to ${type} user:`, error);
      alert(`Failed to ${type} user. Please try again.`);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRefresh = () => {
    fetchData(true);
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

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading user approvals...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="rounded-md bg-red-50 p-4">
            <h3 className="text-sm font-medium text-red-800">Error loading data</h3>
            <p className="mt-2 text-sm text-red-700">{error}</p>
            <button
              onClick={() => fetchData()}
              className="mt-3 text-sm font-medium text-red-600 hover:text-red-500"
            >
              Try again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-4 mb-4">
              <button
                onClick={() => router.push('/')}
                className="inline-flex items-center text-sm text-gray-600 hover:text-gray-900"
              >
                <ArrowLeft size={16} className="mr-1" />
                Back to Home
              </button>
            </div>
            <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:text-3xl sm:tracking-tight">
              User Approvals
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              Manage pending user registrations and approval requests
            </p>
          </div>
          <div className="mt-4 flex gap-3 md:ml-4 md:mt-0">
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw size={16} className={`mr-1 ${refreshing ? 'animate-spin' : ''}`} />
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>

        {/* Statistics Cards */}
        {statistics && (
          <div className="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">Pending Approvals</dt>
              <dd className="mt-1 text-3xl font-semibold text-yellow-600">{statistics.pendingCount}</dd>
            </div>
            <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">Approved Users</dt>
              <dd className="mt-1 text-3xl font-semibold text-green-600">{statistics.approvedCount}</dd>
            </div>
            <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">Rejected Users</dt>
              <dd className="mt-1 text-3xl font-semibold text-red-600">{statistics.rejectedCount}</dd>
            </div>
            <div className="overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">Approval Rate</dt>
              <dd className="mt-1 text-3xl font-semibold text-indigo-600">
                {statistics.approvalRate.toFixed(1)}%
              </dd>
            </div>
          </div>
        )}

        {/* Filters and Search */}
        <div className="mt-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
          <div className="flex gap-2">
            {Object.values(ApprovalStatus).map((status) => (
              <button
                key={status}
                onClick={() => handleStatusChange(status)}
                className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md ${
                  selectedStatus === status
                    ? 'bg-indigo-100 text-indigo-700'
                    : 'bg-white text-gray-500 hover:text-gray-700'
                } border border-gray-300`}
              >
                {getStatusIcon(status)}
                <span className="ml-2">{status}</span>
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
              placeholder="Search by phone number..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>
        </div>

        {/* Users Table */}
        <div className="mt-8 overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg">
          <table className="min-w-full divide-y divide-gray-300">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  User
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Registered
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Days Waiting
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredUsers.map((user) => (
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
                          <div className="text-sm text-red-600">Reason: {user.rejectionReason}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusBadgeColor(user.approvalStatus)}`}>
                      {getStatusIcon(user.approvalStatus)}
                      <span className="ml-1">{user.approvalStatus}</span>
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.daysWaiting} days
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex gap-2">
                      {user.approvalStatus === ApprovalStatus.PENDING && (
                        <>
                          <button
                            onClick={() => openModal('approve', user.phone)}
                            disabled={actionLoading === user.phone}
                            className="text-green-600 hover:text-green-900 disabled:opacity-50"
                            title="Approve User"
                          >
                            <CheckCircle size={18} />
                          </button>
                          <button
                            onClick={() => openModal('reject', user.phone)}
                            disabled={actionLoading === user.phone}
                            className="text-red-600 hover:text-red-900 disabled:opacity-50"
                            title="Reject User"
                          >
                            <XCircle size={18} />
                          </button>
                        </>
                      )}
                      {user.approvalStatus !== ApprovalStatus.PENDING && (
                        <button
                          onClick={() => openModal('reset', user.phone)}
                          disabled={actionLoading === user.phone}
                          className="text-gray-600 hover:text-gray-900 disabled:opacity-50"
                          title="Reset to Pending"
                        >
                          <Clock size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {filteredUsers.length === 0 && (
            <div className="text-center py-12">
              <Users className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No users found</h3>
              <p className="mt-1 text-sm text-gray-500">
                {searchTerm 
                  ? `No users found matching "${searchTerm}"`
                  : `No users with status "${selectedStatus}"`
                }
              </p>
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between">
            <div className="text-sm text-gray-700">
              Showing {filteredUsers.length} of {totalUsers} users
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="px-3 py-2 text-sm font-medium text-gray-700">
                Page {currentPage} of {totalPages}
              </span>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}

        {/* Approval Modal */}
        <ApprovalModal
          isOpen={modalState.isOpen}
          onClose={closeModal}
          onConfirm={handleModalConfirm}
          type={modalState.type}
          userPhone={modalState.userPhone}
          loading={actionLoading === modalState.userPhone}
        />
      </div>
    </div>
  );
}