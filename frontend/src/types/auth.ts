export enum SalesRole {
  ADMIN = 'ADMIN',
  OFFICER = 'OFFICER',
  CUSTOMER_AGENT = 'CUSTOMER_AGENT'
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
  role?: SalesRole;
}

export interface AuthResponse {
  token?: string;
  phone?: string;
  role?: SalesRole;
  error?: string;
  status?: string;
  mustChangePassword?: boolean;
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
  role?: SalesRole;
  approvalStatus: ApprovalStatus;
  createdAt: string;
  statusUpdatedAt: string;
  daysWaiting: number;
  approvedByPhone?: string;
  approvedAt?: string;
  rejectionReason?: string;
  isEnabled?: boolean;
  disabledAt?: string;
  disabledByPhone?: string;
  disabledReason?: string;
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

export interface BulkEnableDisableRequest {
  action: 'ENABLE' | 'DISABLE';
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

export interface PasswordResetResponse {
  temporaryPassword: string;
  userPhone: string;
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

// Helper function to get translated role name
export const getTranslatedRoleName = (role: SalesRole, t: (key: string) => string): string => {
  const roleTranslationMap: Record<SalesRole, string> = {
    [SalesRole.ADMIN]: 'role.admin',
    [SalesRole.OFFICER]: 'role.officer',
    [SalesRole.CUSTOMER_AGENT]: 'role.customerAgent',
  };

  const translationKey = roleTranslationMap[role];
  return translationKey ? t(translationKey) : role;
};

// Customer Delete Request Types
export enum DeleteRequestStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface CustomerDeleteRequest {
  id: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  requestedBy: string;
  requestStatus: DeleteRequestStatus | string; // Backend returns string enum
  reason: string;
  createdAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

export interface DeleteRequestPageResponse {
  items: CustomerDeleteRequest[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface DeleteRequestStatistics {
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  approvalRate: number;
}

export interface CreateDeleteRequestRequest {
  customerId: string;
  reason: string;
}

export interface RejectDeleteRequestRequest {
  rejectionReason: string;
}
