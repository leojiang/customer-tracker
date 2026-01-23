import {
  CustomerDeleteRequest,
  DeleteRequestPageResponse,
  CreateDeleteRequestRequest,
  RejectDeleteRequestRequest,
  DeleteRequestStatistics,
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
    const response = await fetch(`${API_BASE_URL}/customer-delete-requests`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      let errorMessage = 'Failed to create delete request';
      try {
        const error = await response.json();
        errorMessage = error.error || errorMessage;
      } catch (e) {
        // Response body is empty or not JSON
        errorMessage = response.statusText || 'Failed to create delete request';
      }
      throw new Error(errorMessage);
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
      `${API_BASE_URL}/customer-delete-requests/pending?${params}`,
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

    const data = await response.json();
    // Convert Spring Data Page format to our frontend format
    return {
      items: data.content,
      total: data.totalElements,
      page: data.pageable.pageNumber + 1, // Convert 0-based to 1-based
      limit: data.pageable.pageSize,
      totalPages: data.totalPages,
    };
  },

  /**
   * Get all delete requests with status filter (Admin only)
   */
  async getAllDeleteRequests(
    token: string,
    status: 'PENDING' | 'APPROVED' | 'REJECTED',
    page: number = 0,
    pageSize: number = 20,
    sortDir: 'asc' | 'desc' = 'desc'
  ): Promise<DeleteRequestPageResponse> {
    const params = new URLSearchParams({
      status,
      page: page.toString(),
      pageSize: pageSize.toString(),
      sortDir,
    });

    const response = await fetch(
      `${API_BASE_URL}/customer-delete-requests?${params}`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to fetch delete requests');
    }

    const data = await response.json();
    // Convert Spring Data Page format to our frontend format
    return {
      items: data.content,
      total: data.totalElements,
      page: data.pageable.pageNumber + 1, // Convert 0-based to 1-based
      limit: data.pageable.pageSize,
      totalPages: data.totalPages,
    };
  },

  /**
   * Approve a delete request (Admin only)
   */
  async approveDeleteRequest(token: string, requestId: string, reason?: string): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/customer-delete-requests/${requestId}/approve`,
      {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ reason }),
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
      `${API_BASE_URL}/customer-delete-requests/${requestId}/reject`,
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
      `${API_BASE_URL}/customer-delete-requests/${requestId}`,
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
      `${API_BASE_URL}/customer-delete-requests/pending/count`,
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

  /**
   * Get delete request statistics (Admin only)
   */
  async getStatistics(token: string): Promise<DeleteRequestStatistics> {
    const response = await fetch(
      `${API_BASE_URL}/customer-delete-requests/statistics`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error('Failed to fetch statistics');
    }

    return response.json();
  },
};
