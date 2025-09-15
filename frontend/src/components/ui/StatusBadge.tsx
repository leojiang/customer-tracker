'use client';

import { CustomerStatus } from '@/types/customer';
import { useLanguage } from '@/contexts/LanguageContext';

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

const statusTranslationKeys = {
  [CustomerStatus.CUSTOMER_CALLED]: 'status.customerCalled',
  [CustomerStatus.REPLIED_TO_CUSTOMER]: 'status.repliedToCustomer',
  [CustomerStatus.ORDER_PLACED]: 'status.orderPlaced',
  [CustomerStatus.ORDER_CANCELLED]: 'status.orderCancelled',
  [CustomerStatus.PRODUCT_DELIVERED]: 'status.productDelivered',
  [CustomerStatus.BUSINESS_DONE]: 'status.businessDone',
  [CustomerStatus.LOST]: 'status.lost',
};

export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const { t } = useLanguage();
  
  return (
    <span className={`status-badge ${statusStyles[status]} ${className}`}>
      {t(statusTranslationKeys[status])}
    </span>
  );
}