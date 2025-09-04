export enum CustomerStatus {
  CUSTOMER_CALLED = 'CUSTOMER_CALLED',
  REPLIED_TO_CUSTOMER = 'REPLIED_TO_CUSTOMER',
  ORDER_PLACED = 'ORDER_PLACED',
  ORDER_CANCELLED = 'ORDER_CANCELLED',
  PRODUCT_DELIVERED = 'PRODUCT_DELIVERED',
  BUSINESS_DONE = 'BUSINESS_DONE',
  LOST = 'LOST'
}

export const CustomerStatusDisplayNames: Record<CustomerStatus, string> = {
  [CustomerStatus.CUSTOMER_CALLED]: 'Customer called',
  [CustomerStatus.REPLIED_TO_CUSTOMER]: 'Replied to customer',
  [CustomerStatus.ORDER_PLACED]: 'Order placed',
  [CustomerStatus.ORDER_CANCELLED]: 'Order cancelled',
  [CustomerStatus.PRODUCT_DELIVERED]: 'Product delivered',
  [CustomerStatus.BUSINESS_DONE]: 'Business done',
  [CustomerStatus.LOST]: 'Lost'
};

export interface Customer {
  id: string;
  name: string;
  phone: string;
  company?: string;
  businessRequirements?: string;
  businessType?: string;
  age?: number;
  education?: string;
  gender?: string;
  location?: string;
  currentStatus: CustomerStatus;
  salesPhone?: string;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
  deleted: boolean;
}

export interface StatusHistory {
  id: string;
  customer: Customer;
  fromStatus?: CustomerStatus;
  toStatus: CustomerStatus;
  reason?: string;
  changedAt: string;
}

export interface CreateCustomerRequest {
  name: string;
  phone: string;
  company?: string;
  businessRequirements?: string;
  businessType?: string;
  age?: number;
  education?: string;
  gender?: string;
  location?: string;
  currentStatus?: CustomerStatus;
}

export interface UpdateCustomerRequest {
  name: string;
  phone: string;
  company?: string;
  businessRequirements?: string;
  businessType?: string;
  age?: number;
  education?: string;
  gender?: string;
  location?: string;
}

export interface StatusTransitionRequest {
  toStatus: CustomerStatus;
  reason?: string;
}

export interface CustomerPageResponse {
  items: Customer[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface CustomerSearchParams {
  q?: string;
  phone?: string;
  status?: CustomerStatus;
  company?: string;
  includeDeleted?: boolean;
  page?: number;
  limit?: number;
}