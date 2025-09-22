import { 
  Customer, 
  CreateCustomerRequest, 
  UpdateCustomerRequest, 
  StatusTransitionRequest, 
  CustomerPageResponse, 
  CustomerSearchParams, 
  StatusHistory,
  CustomerStatus
} from '@/types/customer';
import { 
  LoginRequest, 
  RegisterRequest, 
  AuthResponse, 
  ValidateTokenRequest,
  ApprovalPageResponse,
  BulkApprovalRequest,
  BulkEnableDisableRequest,
  BulkActionResponse,
  UserApprovalHistory,
  ApprovalStatistics,
  ApprovalStatus,
  UserApprovalDto
} from '@/types/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  
  // Get token from localStorage for authentication
  const token = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
  
  const config: RequestInit = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);
    
    const contentType = response.headers.get('content-type');
    const isJsonResponse = contentType && contentType.includes('application/json');
    
    if (!response.ok) {
      // For auth endpoints, try to return the error response as JSON if possible
      if (endpoint.startsWith('/auth/') && isJsonResponse) {
        const errorData = await response.json();
        return errorData as T;
      }
      
      const errorText = await response.text();
      throw new ApiError(response.status, errorText || `HTTP ${response.status}`);
    }

    if (isJsonResponse) {
      return await response.json();
    }
    
    return response.text() as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, `Network error: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}

export const customerApi = {
  async searchCustomers(params: CustomerSearchParams = {}): Promise<CustomerPageResponse> {
    const searchParams = new URLSearchParams();
    
    if (params.q) {
      searchParams.append('q', params.q);
    }
    if (params.phone) {
      searchParams.append('phone', params.phone);
    }
    if (params.status) {
      searchParams.append('status', params.status);
    }
    if (params.company) {
      searchParams.append('company', params.company);
    }
    if (params.includeDeleted) {
      searchParams.append('includeDeleted', params.includeDeleted.toString());
    }
    if (params.page !== undefined) {
      searchParams.append('page', params.page.toString());
    }
    if (params.limit !== undefined) {
      searchParams.append('limit', params.limit.toString());
    }

    const query = searchParams.toString();
    return fetchApi<CustomerPageResponse>(`/customers${query ? `?${query}` : ''}`);
  },

  async getCustomer(id: string): Promise<Customer> {
    return fetchApi<Customer>(`/customers/${id}`);
  },

  async createCustomer(customer: CreateCustomerRequest): Promise<Customer> {
    return fetchApi<Customer>('/customers', {
      method: 'POST',
      body: JSON.stringify(customer),
    });
  },

  async updateCustomer(id: string, customer: UpdateCustomerRequest): Promise<Customer> {
    return fetchApi<Customer>(`/customers/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(customer),
    });
  },

  async deleteCustomer(id: string): Promise<void> {
    return fetchApi<void>(`/customers/${id}`, {
      method: 'DELETE',
    });
  },

  async transitionStatus(id: string, transition: StatusTransitionRequest): Promise<Customer> {
    return fetchApi<Customer>(`/customers/${id}/status-transition`, {
      method: 'POST',
      body: JSON.stringify(transition),
    });
  },

  async getStatusHistory(customerId: string): Promise<StatusHistory[]> {
    return fetchApi<StatusHistory[]>(`/customers/${customerId}/status-history`);
  },

  async getValidTransitions(customerId: string): Promise<CustomerStatus[]> {
    return fetchApi<CustomerStatus[]>(`/customers/${customerId}/valid-transitions`);
  },

  async canTransitionTo(customerId: string, status: CustomerStatus): Promise<boolean> {
    const response = await fetchApi<{ valid: boolean }>(`/customers/${customerId}/can-transition-to/${status}`);
    return response.valid;
  },
};

// Auth API
export const authApi = {
  async login(loginData: LoginRequest): Promise<AuthResponse> {
    return fetchApi<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(loginData),
    });
  },

  async register(registerData: RegisterRequest): Promise<AuthResponse> {
    return fetchApi<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(registerData),
    });
  },

  async validateToken(tokenData: ValidateTokenRequest): Promise<AuthResponse> {
    return fetchApi<AuthResponse>('/auth/validate', {
      method: 'POST',
      body: JSON.stringify(tokenData),
    });
  },

  logout: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_data');
    }
  },
};

// User Approval API
export const userApprovalApi = {
  async getStatistics(): Promise<ApprovalStatistics> {
    return fetchApi<ApprovalStatistics>('/admin/user-approvals/statistics');
  },

  async getUserApprovals(status: ApprovalStatus = ApprovalStatus.PENDING, page: number = 1, limit: number = 20): Promise<ApprovalPageResponse> {
    const params = new URLSearchParams({
      status,
      page: page.toString(),
      limit: limit.toString(),
    });
    return fetchApi<ApprovalPageResponse>(`/admin/user-approvals?${params}`);
  },

  async getAllUsers(page: number = 1, limit: number = 20): Promise<ApprovalPageResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });
    return fetchApi<ApprovalPageResponse>(`/admin/user-approvals/all-users?${params}`);
  },

  async getApprovedUsersByEnabledStatus(
    enabled: boolean, 
    page: number = 1, 
    limit: number = 20
  ): Promise<ApprovalPageResponse> {
    const params = new URLSearchParams({
      enabled: enabled.toString(),
      page: page.toString(),
      limit: limit.toString(),
    });
    return fetchApi<ApprovalPageResponse>(`/admin/user-approvals/approved-users?${params}`);
  },

  async approveUser(phone: string, reason?: string): Promise<UserApprovalDto> {
    return fetchApi<UserApprovalDto>(`/admin/user-approvals/${phone}/approve`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  async rejectUser(phone: string, reason?: string): Promise<UserApprovalDto> {
    return fetchApi<UserApprovalDto>(`/admin/user-approvals/${phone}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  async resetUser(phone: string, reason?: string): Promise<UserApprovalDto> {
    return fetchApi<UserApprovalDto>(`/admin/user-approvals/${phone}/reset`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  async enableUser(phone: string, reason?: string): Promise<UserApprovalDto> {
    return fetchApi<UserApprovalDto>(`/admin/user-approvals/${phone}/enable`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  async disableUser(phone: string, reason?: string): Promise<UserApprovalDto> {
    return fetchApi<UserApprovalDto>(`/admin/user-approvals/${phone}/disable`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  async bulkAction(request: BulkApprovalRequest): Promise<BulkActionResponse> {
    return fetchApi<BulkActionResponse>('/admin/user-approvals/bulk-action', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async bulkEnableDisable(request: BulkEnableDisableRequest): Promise<BulkActionResponse> {
    return fetchApi<BulkActionResponse>('/admin/user-approvals/bulk-enable-disable', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async getUserHistory(phone: string): Promise<UserApprovalHistory[]> {
    return fetchApi<UserApprovalHistory[]>(`/admin/user-approvals/${phone}/history`);
  },

  async getRecentActivity(days: number = 7): Promise<UserApprovalHistory[]> {
    return fetchApi<UserApprovalHistory[]>(`/admin/user-approvals/activity?days=${days}`);
  },
};

export { ApiError };