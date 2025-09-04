'use client';

import { CustomerStatus, CustomerStatusDisplayNames } from '@/types/customer';

interface StatusBadgeProps {
  status: CustomerStatus;
  className?: string;
}

const statusStyles = {
  [CustomerStatus.CUSTOMER_CALLED]: 'status-customer-called',
  [CustomerStatus.REPLIED_TO_CUSTOMER]: 'status-replied-to-customer',
  [CustomerStatus.ORDER_PLACED]: 'status-order-placed',
  [CustomerStatus.ORDER_CANCELLED]: 'status-order-cancelled',
  [CustomerStatus.PRODUCT_DELIVERED]: 'status-product-delivered',
  [CustomerStatus.BUSINESS_DONE]: 'status-business-done',
  [CustomerStatus.LOST]: 'status-lost',
};

export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  return (
    <span className={`status-badge ${statusStyles[status]} ${className}`}>
      {CustomerStatusDisplayNames[status]}
    </span>
  );
}