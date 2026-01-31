'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';

/**
 * Dashboard router page that redirects users to role-specific dashboards
 */
export default function Dashboard() {
  const { user } = useAuth();
  const { t } = useLanguage();
  const router = useRouter();

  useEffect(() => {
    if (user) {
      // All authenticated users go to admin dashboard
      router.push('/dashboard/admin');
    } else {
      router.push('/auth');
    }
  }, [user, router]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">{t('dashboard.metrics.loadingDashboard')}</p>
      </div>
    </div>
  );
}