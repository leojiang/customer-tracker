export enum CustomerStatus {
  CUSTOMER_CALLED = 'CUSTOMER_CALLED',
  REPLIED_TO_CUSTOMER = 'REPLIED_TO_CUSTOMER',
  ORDER_PLACED = 'ORDER_PLACED',
  ORDER_CANCELLED = 'ORDER_CANCELLED',
  PRODUCT_DELIVERED = 'PRODUCT_DELIVERED',
  BUSINESS_DONE = 'BUSINESS_DONE',
  LOST = 'LOST'
}

export enum EducationLevel {
  ELEMENTARY = 'ELEMENTARY',
  MIDDLE_SCHOOL = 'MIDDLE_SCHOOL',
  HIGH_SCHOOL = 'HIGH_SCHOOL',
  ASSOCIATE = 'ASSOCIATE',
  BACHELOR = 'BACHELOR',
  MASTER = 'MASTER',
  DOCTORATE = 'DOCTORATE',
  PROFESSIONAL = 'PROFESSIONAL',
  CERTIFICATE = 'CERTIFICATE',
  OTHER = 'OTHER'
}

export enum CertificateType {
  ELECTRICIAN = 'ELECTRICIAN',
  WELDER = 'WELDER',
  EXCAVATOR = 'EXCAVATOR'
}

export const EducationLevelDisplayNames: Record<EducationLevel, string> = {
  [EducationLevel.ELEMENTARY]: 'Elementary School',
  [EducationLevel.MIDDLE_SCHOOL]: 'Middle School',
  [EducationLevel.HIGH_SCHOOL]: 'High School',
  [EducationLevel.ASSOCIATE]: 'Associate Degree',
  [EducationLevel.BACHELOR]: 'Bachelor\'s Degree',
  [EducationLevel.MASTER]: 'Master\'s Degree',
  [EducationLevel.DOCTORATE]: 'Doctorate/PhD',
  [EducationLevel.PROFESSIONAL]: 'Professional Degree',
  [EducationLevel.CERTIFICATE]: 'Certificate/Diploma',
  [EducationLevel.OTHER]: 'Other'
};

export const CertificateTypeTranslationKeys: Record<CertificateType, string> = {
  [CertificateType.ELECTRICIAN]: 'certificateType.electrician',
  [CertificateType.WELDER]: 'certificateType.welder',
  [CertificateType.EXCAVATOR]: 'certificateType.excavator'
};

export const CustomerStatusDisplayNames: Record<CustomerStatus, string> = {
  [CustomerStatus.CUSTOMER_CALLED]: 'Customer called',
  [CustomerStatus.REPLIED_TO_CUSTOMER]: 'Replied to customer',
  [CustomerStatus.ORDER_PLACED]: 'Order placed',
  [CustomerStatus.ORDER_CANCELLED]: 'Order cancelled',
  [CustomerStatus.PRODUCT_DELIVERED]: 'Product delivered',
  [CustomerStatus.BUSINESS_DONE]: 'Business done',
  [CustomerStatus.LOST]: 'Lost'
};

// Status translation keys mapping for use with translation function
export const CustomerStatusTranslationKeys: Record<CustomerStatus, string> = {
  [CustomerStatus.CUSTOMER_CALLED]: 'status.customerCalled',
  [CustomerStatus.REPLIED_TO_CUSTOMER]: 'status.repliedToCustomer',
  [CustomerStatus.ORDER_PLACED]: 'status.orderPlaced',
  [CustomerStatus.ORDER_CANCELLED]: 'status.orderCancelled',
  [CustomerStatus.PRODUCT_DELIVERED]: 'status.productDelivered',
  [CustomerStatus.BUSINESS_DONE]: 'status.businessDone',
  [CustomerStatus.LOST]: 'status.lost',
};

// Education level translation keys mapping for use with translation function
export const EducationLevelTranslationKeys: Record<EducationLevel, string> = {
  [EducationLevel.ELEMENTARY]: 'education.elementary',
  [EducationLevel.MIDDLE_SCHOOL]: 'education.middleSchool',
  [EducationLevel.HIGH_SCHOOL]: 'education.highSchool',
  [EducationLevel.ASSOCIATE]: 'education.associate',
  [EducationLevel.BACHELOR]: 'education.bachelor',
  [EducationLevel.MASTER]: 'education.master',
  [EducationLevel.DOCTORATE]: 'education.doctorate',
  [EducationLevel.PROFESSIONAL]: 'education.professional',
  [EducationLevel.CERTIFICATE]: 'education.certificate',
  [EducationLevel.OTHER]: 'education.other',
};

// Helper function to get translated status name
export const getTranslatedStatusName = (status: string, t: (key: string) => string): string => {
  // Check if the status exists in our enum
  if (Object.values(CustomerStatus).includes(status as CustomerStatus)) {
    return t(CustomerStatusTranslationKeys[status as CustomerStatus]);
  }
  // Fallback to formatted string if not in our enum
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, l => l.toUpperCase());
};

// Helper function to get translated education level name
export const getTranslatedEducationLevelName = (educationLevel: EducationLevel, t: (key: string) => string): string => {
  return t(EducationLevelTranslationKeys[educationLevel]);
};

export interface Customer {
  id: string;
  name: string;
  phone: string;
  company?: string;
  businessRequirements?: string;
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  location?: string;
  price?: number;
  currentStatus: CustomerStatus;
  salesPhone?: string;
  certifiedAt?: string;
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
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  location?: string;
  price?: number;
  currentStatus?: CustomerStatus;
  certifiedAt?: string;
}

export interface UpdateCustomerRequest {
  name: string;
  phone: string;
  company?: string;
  businessRequirements?: string;
  certificateType?: CertificateType;
  age?: number;
  education?: EducationLevel;
  gender?: string;
  location?: string;
  price?: number;
  certifiedAt?: string;
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
  certifiedStartDate?: string;
  certifiedEndDate?: string;
  page?: number;
  limit?: number;
}
