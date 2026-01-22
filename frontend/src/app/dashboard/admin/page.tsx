'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useRouter } from 'next/navigation';
import StatusDistributionChart from '@/components/dashboard/charts/StatusDistributionChart';
import TrendLineChart from '@/components/dashboard/charts/TrendLineChart';
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
  const { t } = useLanguage();
  const router = useRouter();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardResponse | null>(null);
  const [trends, setTrends] = useState<TrendAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      setLoading(true);
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
    }
  }, [token]);

  useEffect(() => {
    if (!user || !token) {
      router.push('/auth');
      return;
    }

    if (user.role !== 'ADMIN') {
      router.push('/dashboard/sales');
      return;
    }

    fetchDashboardData();
  }, [user, token, router, fetchDashboardData]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">{t('dashboard.metrics.loadingDashboard')}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="rounded-md bg-red-50 p-4">
            <h3 className="text-sm font-medium text-red-800">{t('dashboard.metrics.errorLoadingDashboard')}</h3>
            <p className="mt-2 text-sm text-red-700">{error}</p>
            <button
              onClick={() => fetchDashboardData()}
              className="mt-3 text-sm font-medium text-red-600 hover:text-red-500"
            >
              {t('customers.tryAgain')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="px-8 py-6 space-y-8">
      <div>
        {/* KPI Cards */}
        <div className="mt-4 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title={t('dashboard.metrics.totalCustomers')}
            value={overview?.totalCustomers || 0}
            change={overview?.periodChange.totalCustomersChange}
            description={t('dashboard.metrics.fromLastPeriod')}
            loading={loading}
          />
          
          <MetricCard
            title={t('dashboard.metrics.newCustomers30d')}
            value={overview?.newCustomersThisPeriod || 0}
            change={overview?.periodChange.newCustomersChange}
            description={t('dashboard.metrics.fromLastPeriod')}
            loading={loading}
          />
          
          <MetricCard
            title="Active Customers"
            value={overview?.activeCustomers || 0}
            description={t('dashboard.metrics.recentActivity')}
            loading={loading}
          />
          
          <MetricCard
            title={t('dashboard.metrics.conversionRate')}
            value={overview?.conversionRate ? `${overview.conversionRate.toFixed(1)}%` : '0%'}
            change={overview?.periodChange.conversionRateChange}
            description={t('dashboard.metrics.fromLastPeriod')}
            loading={loading}
          />
        </div>

        {/* Charts Section */}
        <div className="mt-8 space-y-8">
          {/* Customer Trends Chart */}
          <TrendLineChart 
            data={trends?.dataPoints || []}
            title={t('dashboard.charts.trends')}
            granularity={trends?.granularity || 'daily'}
            loading={loading}
            error={error}
          />

          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Status Distribution Chart */}
            <StatusDistributionChart 
              data={statusDistribution?.statusCounts || {}}
              totalCustomers={statusDistribution?.totalCustomers || 0}
              title={t('dashboard.charts.statusDistribution')}
              loading={loading}
              error={error}
            />

            {/* Sales Leaderboard */}
            <div className="overflow-hidden rounded-lg bg-white shadow flex flex-col">
              <div className="p-6 flex-shrink-0">
                <h3 className="text-lg font-medium leading-6 text-gray-900">{t('dashboard.charts.leaderboard')}</h3>
              </div>
              <div className="flex-1 px-6 pb-6">
                {loading ? (
                  <div className="animate-pulse h-full">
                    <div className="h-full overflow-y-auto">
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
                  </div>
                ) : error ? (
                  <div className="h-full flex items-center justify-center text-center text-red-500">
                    <div>
                      <p>Error loading leaderboard</p>
                      <p className="text-sm text-gray-500 mt-1">{error}</p>
                    </div>
                  </div>
                ) : leaderboard && leaderboard.rankings.length > 0 ? (
                  <div className="h-full overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
                    <div className="space-y-4">
                      {leaderboard.rankings.map((entry) => (
                        <div key={entry.salesPhone} className="flex items-center justify-between hover:bg-gray-50 rounded-lg p-3 transition-colors duration-200">
                          <div className="flex items-center">
                            <span className="flex-shrink-0 w-8 h-8 bg-indigo-100 text-indigo-800 text-sm font-medium rounded-full flex items-center justify-center">
                              #{entry.rank}
                            </span>
                            <div className="ml-3">
                              <p className="text-sm font-medium text-gray-900">{entry.salesPhone}</p>
                              <p className="text-sm text-gray-500">{entry.totalCustomers} {t('dashboard.charts.customers')}</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-medium text-gray-900">{entry.conversions} {t('dashboard.sales.conversions')}</p>
                            <p className="text-sm text-gray-500">{entry.conversionRate.toFixed(1)}% {t('dashboard.sales.rate')}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center text-center text-gray-500">
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