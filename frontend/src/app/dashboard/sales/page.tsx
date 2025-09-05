'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Users, RefreshCw } from 'lucide-react';
import MetricCard from '@/components/dashboard/widgets/MetricCard';

interface DashboardOverview {
  totalCustomers: number;
  newCustomersThisPeriod: number;
  activeCustomers: number;
  conversionRate: number;
  periodChange: {
    totalCustomersChange: number;
    newCustomersChange: number;
    conversionRateChange: number;
  };
}

interface StatusDistribution {
  statusCounts: Record<string, number>;
  totalCustomers: number;
}

interface SalesPerformance {
  salesPhone: string;
  totalCustomers: number;
  newCustomers: number;
  conversions: number;
  conversionRate: number;
  statusBreakdown: Record<string, number>;
}

/**
 * Sales Dashboard showing personal performance metrics
 */
export default function SalesDashboard() {
  const { user, token } = useAuth();
  const router = useRouter();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution | null>(null);
  const [performance, setPerformance] = useState<SalesPerformance | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchDashboardData = useCallback(async (isRefresh = false) => {
    if (!token) {
      return;
    }

    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      // Fetch all dashboard data in parallel
      const [overviewRes, statusRes, performanceRes] = await Promise.all([
        fetch('http://localhost:8080/api/analytics/dashboard/overview', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch('http://localhost:8080/api/analytics/customers/status-distribution', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch('http://localhost:8080/api/analytics/sales/performance', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
      ]);

      if (!overviewRes.ok || !statusRes.ok || !performanceRes.ok) {
        throw new Error('Failed to fetch dashboard data');
      }

      const [overviewData, statusData, performanceData] = await Promise.all([
        overviewRes.json(),
        statusRes.json(),
        performanceRes.json(),
      ]);

      setOverview(overviewData);
      setStatusDistribution(statusData);
      setPerformance(performanceData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [token]);

  useEffect(() => {
    if (!user || !token) {
      router.push('/auth');
      return;
    }

    if (user.role !== 'SALES') {
      router.push('/dashboard/admin');
      return;
    }

    fetchDashboardData();
  }, [user, token, router, fetchDashboardData]);

  const handleRefresh = () => {
    fetchDashboardData(true);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="rounded-md bg-red-50 p-4">
            <h3 className="text-sm font-medium text-red-800">Error loading dashboard</h3>
            <p className="mt-2 text-sm text-red-700">{error}</p>
            <button
              onClick={() => fetchDashboardData()}
              className="mt-3 text-sm font-medium text-red-600 hover:text-red-500"
            >
              Try again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-4 mb-4">
              <button
                onClick={() => router.push('/')}
                className="inline-flex items-center text-sm text-gray-600 hover:text-gray-900"
              >
                <ArrowLeft size={16} className="mr-1" />
                Back to Customers
              </button>
            </div>
            <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:text-3xl sm:tracking-tight">
              My Dashboard
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              Your personal performance metrics and customer pipeline
            </p>
          </div>
          <div className="mt-4 flex gap-3 md:ml-4 md:mt-0">
            <button
              onClick={() => router.push('/')}
              className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
            >
              <Users size={16} className="mr-1" />
              View Customers
            </button>
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw size={16} className={`mr-1 ${refreshing ? 'animate-spin' : ''}`} />
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>

        {/* KPI Cards */}
        <div className="mt-8 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title="My Customers"
            value={overview?.totalCustomers || 0}
            change={overview?.periodChange.totalCustomersChange}
            description="from last period"
            loading={loading}
          />
          
          <MetricCard
            title="New This Month"
            value={overview?.newCustomersThisPeriod || 0}
            change={overview?.periodChange.newCustomersChange}
            description="from last period"
            loading={loading}
          />
          
          <MetricCard
            title="Active Customers"
            value={overview?.activeCustomers || 0}
            description="Recent activity in last 30 days"
            loading={loading}
          />
          
          <MetricCard
            title="My Conversion Rate"
            value={overview?.conversionRate ? `${overview.conversionRate.toFixed(1)}%` : '0%'}
            change={overview?.periodChange.conversionRateChange}
            description="from last period"
            loading={loading}
          />
        </div>

        <div className="mt-8 grid grid-cols-1 gap-8 lg:grid-cols-2">
          {/* My Pipeline Status */}
          {statusDistribution && (
            <div className="overflow-hidden rounded-lg bg-white shadow">
              <div className="p-6">
                <h3 className="text-lg font-medium leading-6 text-gray-900">My Pipeline Status</h3>
                <div className="mt-6">
                  {Object.entries(statusDistribution.statusCounts).map(([status, count]) => (
                    <div key={status} className="flex items-center justify-between py-3 border-b border-gray-200 last:border-b-0">
                      <span className="text-sm font-medium text-gray-700">
                        {status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </span>
                      <div className="flex items-center">
                        <div className="w-32 bg-gray-200 rounded-full h-2 mr-2">
                          <div 
                            className="bg-indigo-600 h-2 rounded-full" 
                            style={{ width: `${((count / statusDistribution.totalCustomers) * 100)}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-gray-900 w-8 text-right">{count}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Performance Summary */}
          {performance && (
            <div className="overflow-hidden rounded-lg bg-white shadow">
              <div className="p-6">
                <h3 className="text-lg font-medium leading-6 text-gray-900">Performance Summary</h3>
                <div className="mt-6 space-y-6">
                  <div>
                    <div className="flex justify-between text-sm">
                      <span className="font-medium text-gray-700">Conversion Progress</span>
                      <span className="text-gray-500">{performance.conversions} / {performance.totalCustomers}</span>
                    </div>
                    <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
                      <div 
                        className="bg-green-600 h-2 rounded-full" 
                        style={{ width: `${performance.conversionRate}%` }}
                      ></div>
                    </div>
                    <p className="mt-1 text-sm text-gray-500">
                      {performance.conversionRate.toFixed(1)}% conversion rate
                    </p>
                  </div>

                  <div className="border-t border-gray-200 pt-6">
                    <h4 className="text-sm font-medium text-gray-700 mb-4">Key Metrics</h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="text-center">
                        <p className="text-2xl font-semibold text-gray-900">{performance.newCustomers}</p>
                        <p className="text-xs text-gray-500">New Customers</p>
                      </div>
                      <div className="text-center">
                        <p className="text-2xl font-semibold text-gray-900">{performance.conversions}</p>
                        <p className="text-xs text-gray-500">Conversions</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Quick Actions */}
        <div className="mt-8">
          <div className="rounded-lg bg-white shadow p-6">
            <h3 className="text-lg font-medium leading-6 text-gray-900 mb-4">Quick Actions</h3>
            <div className="flex flex-wrap gap-3">
              <button 
                onClick={() => router.push('/')}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
              >
                View All Customers
              </button>
              <button 
                onClick={() => router.push('/?status=CUSTOMER_CALLED')}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
              >
                Follow Up Calls
              </button>
              <button 
                onClick={() => router.push('/?status=REPLIED_TO_CUSTOMER')}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
              >
                Pending Orders
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}