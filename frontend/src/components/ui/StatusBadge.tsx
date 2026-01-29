'use client';

import { CustomerStatus } from '@/types/customer';
import { useLanguage } from '@/contexts/LanguageContext';

interface StatusBadgeProps {
  status: CustomerStatus;
  className?: string;
}

const statusStyles = {
  [CustomerStatus.NEW]: 'status-new',
  [CustomerStatus.NOTIFIED]: 'status-notified',
  [CustomerStatus.ABORTED]: 'status-aborted',
  [CustomerStatus.SUBMITTED]: 'status-submitted',
  [CustomerStatus.CERTIFIED]: 'status-certified',
  [CustomerStatus.CERTIFIED_ELSEWHERE]: 'status-certified-elsewhere',
};

const statusTranslationKeys = {
  [CustomerStatus.NEW]: 'status.new',
  [CustomerStatus.NOTIFIED]: 'status.notified',
  [CustomerStatus.ABORTED]: 'status.aborted',
  [CustomerStatus.SUBMITTED]: 'status.submitted',
  [CustomerStatus.CERTIFIED]: 'status.certified',
  [CustomerStatus.CERTIFIED_ELSEWHERE]: 'status.certifiedElsewhere',
};

export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const { t } = useLanguage();

  return (
    <span className={`status-badge ${statusStyles[status]} ${className}`}>
      {t(statusTranslationKeys[status])}
    </span>
  );
}
