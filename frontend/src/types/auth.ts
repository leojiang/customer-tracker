export enum SalesRole {
  ADMIN = 'ADMIN',
  SALES = 'SALES'
}

export interface Sales {
  phone: string;
  role: SalesRole;
}

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface RegisterRequest {
  phone: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  token?: string;
  phone?: string;
  role?: SalesRole;
  error?: string;
  status?: string;
}

export interface ValidateTokenRequest {
  token: string;
}

// User Approval System Types
export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface UserApprovalDto {
  phone: string;
  approvalStatus: ApprovalStatus;
  createdAt: string;
  statusUpdatedAt: string;
  daysWaiting: number;
  approvedByPhone?: string;
  approvedAt?: string;
  rejectionReason?: string;
}

export interface ApprovalPageResponse {
  items: UserApprovalDto[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface ApprovalActionRequest {
  reason?: string;
}

export interface BulkApprovalRequest {
  action: 'APPROVE' | 'REJECT';
  phones: string[];
  reason?: string;
}

export interface BulkActionResponse {
  successCount: number;
  totalCount: number;
  message: string;
}

export interface UserApprovalHistory {
  id: string;
  userPhone: string;
  action: string;
  adminPhone: string;
  reason?: string;
  actionTimestamp: string;
}

export interface ApprovalStatistics {
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  recentActivityCount: number;
  approvalRate: number;
}