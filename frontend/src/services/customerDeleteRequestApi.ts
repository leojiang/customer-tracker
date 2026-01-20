import {
  CustomerDeleteRequest,
  DeleteRequestPageResponse,
  CreateDeleteRequestRequest,
  RejectDeleteRequestRequest,
} from '@/types/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * API service for customer delete requests
 */
export const customerDeleteRequestApi = {
  /**
   * Create a new delete request for a customer
   */
  async createDeleteRequest(
    token: string,
    request: CreateDeleteRequestRequest
  ): Promise<CustomerDeleteRequest> {
    const response = await fetch(`${API_BASE_URL}/api/customer-delete-requests`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create delete request');
    }

    return response.json();
  },

  /**
   * Get pending delete requests (Admin only)
   */
  async getPendingDeleteRequests(
    token: string,
    page: number = 0,
    pageSize: number = 20,
    sortDir: 'asc' | 'desc' = 'desc'
  ): Promise<DeleteRequestPageResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      pageSize: pageSize.toString(),
      sortDir,
    });

    const response = await fetch(
      `${API_BASE_URL}/api/customer-delete-requests/pending?${params}`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to fetch pending delete requests');
    }

    return response.json();
  },

  /**
   * Approve a delete request (Admin only)
   */
  async approveDeleteRequest(token: string, requestId: string): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/api/customer-delete-requests/${requestId}/approve`,
      {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to approve delete request');
    }
  },

  /**
   * Reject a delete request (Admin only)
   */
  async rejectDeleteRequest(
    token: string,
    requestId: string,
    request: RejectDeleteRequestRequest
  ): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/api/customer-delete-requests/${requestId}/reject`,
      {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(request),
      }
    );

    if (!response.ok) {
      throw new Error('Failed to reject delete request');
    }
  },

  /**
   * Get a specific delete request by ID (Admin only)
   */
  async getDeleteRequest(token: string, requestId: string): Promise<CustomerDeleteRequest> {
    const response = await fetch(
      `${API_BASE_URL}/api/customer-delete-requests/${requestId}`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to fetch delete request');
    }

    return response.json();
  },

  /**
   * Count pending delete requests (Admin only)
   */
  async countPendingRequests(token: string): Promise<number> {
    const response = await fetch(
      `${API_BASE_URL}/api/customer-delete-requests/pending/count`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to count pending requests');
    }

    const count = await response.json();
    return count as number;
  },
};
