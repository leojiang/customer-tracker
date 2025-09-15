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

// Approval status translation keys mapping for use with translation function
export const ApprovalStatusTranslationKeys: Record<ApprovalStatus, string> = {
  [ApprovalStatus.PENDING]: 'approvalStatus.pending',
  [ApprovalStatus.APPROVED]: 'approvalStatus.approved',
  [ApprovalStatus.REJECTED]: 'approvalStatus.rejected',
};

// Helper function to get translated approval status name
export const getTranslatedApprovalStatusName = (status: ApprovalStatus, t: (key: string) => string): string => {
  // Check if the status exists in our enum
  if (Object.values(ApprovalStatus).includes(status)) {
    return t(ApprovalStatusTranslationKeys[status]);
  }
  // Fallback to formatted string if not in our enum
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, l => l.toUpperCase());
};