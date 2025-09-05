'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';

/**
 * Dashboard router page that redirects users to role-specific dashboards
 */
export default function Dashboard() {
  const { user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    console.log('Dashboard router - user:', user);
    if (user) {
      // Redirect to role-specific dashboard
      console.log('User role:', user.role);
      if (user.role === 'ADMIN') {
        console.log('Redirecting to admin dashboard');
        router.push('/dashboard/admin');
      } else {
        console.log('Redirecting to sales dashboard');
        router.push('/dashboard/sales');
      }
    } else {
      console.log('No user, redirecting to auth');
      router.push('/auth');
    }
  }, [user, router]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">Loading dashboard...</p>
      </div>
    </div>
  );
}