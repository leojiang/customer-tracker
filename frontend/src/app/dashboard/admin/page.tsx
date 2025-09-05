'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Users, RefreshCw } from 'lucide-react';
import StatusDistributionChart from '@/components/dashboard/charts/StatusDistributionChart';
import TrendLineChart from '@/components/dashboard/charts/TrendLineChart';
import MetricCard from '@/components/dashboard/widgets/MetricCard';
import ActivityFeed from '@/components/dashboard/widgets/ActivityFeed';

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

interface LeaderboardEntry {
  salesPhone: string;
  totalCustomers: number;
  conversions: number;
  conversionRate: number;
  rank: number;
}

interface LeaderboardResponse {
  rankings: LeaderboardEntry[];
  totalDays: number;
  metric: string;
}

interface TrendDataPoint {
  date: string;
  newCustomers: number;
  totalCustomers: number;
  conversionRate: number;
}

interface TrendAnalysisResponse {
  dataPoints: TrendDataPoint[];
  granularity: string;
  totalDays: number;
}

/**
 * Admin Dashboard showing system-wide analytics and metrics
 */
export default function AdminDashboard() {
  const { user, token } = useAuth();
  const router = useRouter();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardResponse | null>(null);
  const [trends, setTrends] = useState<TrendAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    console.log('Admin dashboard - user:', user, 'token:', token);
    
    if (!user || !token) {
      console.log('No user or token, redirecting to auth');
      router.push('/auth');
      return;
    }

    if (user.role !== 'ADMIN') {
      console.log('User is not admin, redirecting to sales dashboard');
      router.push('/dashboard/sales');
      return;
    }

    console.log('Admin user authenticated, fetching dashboard data');
    fetchDashboardData();
  }, [user, token, router]);

  const fetchDashboardData = async (isRefresh = false) => {
    if (!token) return;

    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      // Fetch all dashboard data in parallel
      const [overviewRes, statusRes, leaderboardRes, trendsRes] = await Promise.all([
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
        fetch('http://localhost:8080/api/analytics/sales/leaderboard', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch('http://localhost:8080/api/analytics/customers/trends?days=30&granularity=daily', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
      ]);

      if (!overviewRes.ok || !statusRes.ok || !leaderboardRes.ok || !trendsRes.ok) {
        throw new Error('Failed to fetch dashboard data');
      }

      const [overviewData, statusData, leaderboardData, trendsData] = await Promise.all([
        overviewRes.json(),
        statusRes.json(),
        leaderboardRes.json(),
        trendsRes.json(),
      ]);

      setOverview(overviewData);
      setStatusDistribution(statusData);
      setLeaderboard(leaderboardData);
      setTrends(trendsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

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
              Admin Dashboard
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              System-wide analytics and performance metrics
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
            title="Total Customers"
            value={overview?.totalCustomers || 0}
            change={overview?.periodChange.totalCustomersChange}
            description="from last period"
            loading={loading}
          />
          
          <MetricCard
            title="New Customers (30d)"
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
            title="Conversion Rate"
            value={overview?.conversionRate ? `${overview.conversionRate.toFixed(1)}%` : '0%'}
            change={overview?.periodChange.conversionRateChange}
            description="from last period"
            loading={loading}
          />
        </div>

        {/* Charts Section */}
        <div className="mt-8 space-y-8">
          {/* Customer Trends Chart */}
          <TrendLineChart 
            data={trends?.dataPoints || []}
            title="Customer Growth Trends"
            granularity={trends?.granularity || 'daily'}
            days={trends?.totalDays || 30}
            loading={loading}
            error={error}
          />

          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Status Distribution Chart */}
            <StatusDistributionChart 
              data={statusDistribution?.statusCounts || {}}
              totalCustomers={statusDistribution?.totalCustomers || 0}
              title="Customer Status Distribution"
              loading={loading}
              error={error}
            />

            {/* Sales Leaderboard */}
            <div className="overflow-hidden rounded-lg bg-white shadow">
              <div className="p-6">
                <h3 className="text-lg font-medium leading-6 text-gray-900">Sales Team Leaderboard</h3>
                {loading ? (
                  <div className="animate-pulse mt-6">
                    {[1, 2, 3, 4, 5].map(i => (
                      <div key={i} className="flex items-center justify-between mb-4 last:mb-0">
                        <div className="flex items-center">
                          <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
                          <div className="ml-3">
                            <div className="h-4 bg-gray-200 rounded w-24 mb-1"></div>
                            <div className="h-3 bg-gray-200 rounded w-16"></div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="h-4 bg-gray-200 rounded w-20 mb-1"></div>
                          <div className="h-3 bg-gray-200 rounded w-12"></div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : error ? (
                  <div className="mt-6 text-center text-red-500">
                    <p>Error loading leaderboard</p>
                    <p className="text-sm text-gray-500 mt-1">{error}</p>
                  </div>
                ) : leaderboard && leaderboard.rankings.length > 0 ? (
                  <div className="mt-6">
                    <div className="space-y-4">
                      {leaderboard.rankings.map((entry) => (
                        <div key={entry.salesPhone} className="flex items-center justify-between">
                          <div className="flex items-center">
                            <span className="flex-shrink-0 w-8 h-8 bg-indigo-100 text-indigo-800 text-sm font-medium rounded-full flex items-center justify-center">
                              #{entry.rank}
                            </span>
                            <div className="ml-3">
                              <p className="text-sm font-medium text-gray-900">{entry.salesPhone}</p>
                              <p className="text-sm text-gray-500">{entry.totalCustomers} customers</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-medium text-gray-900">{entry.conversions} conversions</p>
                            <p className="text-sm text-gray-500">{entry.conversionRate.toFixed(1)}% rate</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="mt-6 text-center text-gray-500">
                    <p>No leaderboard data available</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}