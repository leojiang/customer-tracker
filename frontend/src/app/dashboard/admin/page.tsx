'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useRouter } from 'next/navigation';
import StatusDistributionChart from '@/components/dashboard/charts/StatusDistributionChart';
import TrendLineChart from '@/components/dashboard/charts/TrendLineChart';
import CertificateTypeTrendsChart from '@/components/dashboard/charts/CertificateTypeTrendsChart';
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
  newCustomers: number; // This now represents certifications based on certifiedAt
  totalCustomers: number;
  conversionRate: number;
}

interface TrendAnalysisResponse {
  dataPoints: TrendDataPoint[];
  granularity: string;
  totalDays: number;
}

interface CertificateTypeTrendsResponse {
  trendsByCertificateType: Record<string, TrendDataPoint[]>;
  totalDays: number;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

/**
 * Admin Dashboard showing system-wide analytics and metrics
 */
export default function AdminDashboard() {
  const { user, token } = useAuth();
  const { t } = useLanguage();
  const router = useRouter();

  // Helper functions for localStorage (similar to customer list)
  const STORAGE_KEY = 'adminDashboardFilters';

  interface StoredFilters {
    selectedYear: number;
    selectedMonth: number | null;
    trendsViewOption: string; // 'newCertifications' or 'totalCustomers'
    certificateTypes: string[]; // Array of selected certificate types
  }

  const loadStoredFilters = (): StoredFilters | null => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (error) {
      console.error('Error loading filters from localStorage:', error);
    }
    return null;
  };

  const saveFiltersToStorage = useCallback((filters: StoredFilters) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
    } catch (error) {
      console.error('Error saving filters to localStorage:', error);
    }
  }, []);

  const clearFiltersFromStorage = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch (error) {
      console.error('Error clearing filters from localStorage:', error);
    }
  }, []);

  // Initialize state from localStorage or defaults
  const storedFilters = loadStoredFilters();
  const initialSelectedYear = storedFilters?.selectedYear ?? new Date().getFullYear();
  const initialSelectedMonth = storedFilters?.selectedMonth ?? null;
  const initialTrendsViewOption = storedFilters?.trendsViewOption ?? 'newCertifications';
  const initialCertificateTypes = storedFilters?.certificateTypes ?? [];

  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardResponse | null>(null);
  const [trends, setTrends] = useState<TrendAnalysisResponse | null>(null);
  const [certificateTrends, setCertificateTrends] = useState<CertificateTypeTrendsResponse | null>(null);

  // Individual loading states for each chart/data set
  const [overviewLoading, setOverviewLoading] = useState(true);
  const [statusDistributionLoading, setStatusDistributionLoading] = useState(true);
  const [trendsLoading, setTrendsLoading] = useState(true);
  const [certificateTrendsLoading, setCertificateTrendsLoading] = useState(true);
  const [leaderboardLoading, setLeaderboardLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  // Leaderboard month selection state (initialized from localStorage)
  const [selectedYear, setSelectedYear] = useState<number>(initialSelectedYear);
  const [selectedMonth, setSelectedMonth] = useState<number | null>(initialSelectedMonth);

  // Chart filter states (initialized from localStorage)
  const [trendsViewOption, setTrendsViewOption] = useState<string>(initialTrendsViewOption);
  const [certificateTypes, setCertificateTypes] = useState<string[]>(initialCertificateTypes);

  // Fetch leaderboard by selected month (or year if month is null)
  const fetchLeaderboardByMonth = useCallback(async (year: number, month: number | null) => {
    if (!token) {
      return;
    }

    try {
      setLeaderboardLoading(true);

      // If month is null, fetch yearly ranking; otherwise fetch monthly ranking
      const url = month === null
        ? `${API_BASE_URL}/analytics/sales/leaderboard/yearly?year=${year}&metric=conversions`
        : `${API_BASE_URL}/analytics/sales/leaderboard/monthly?year=${year}&month=${month + 1}&metric=conversions`;

      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const leaderboardData = await response.json();
        setLeaderboard(leaderboardData);
        setLeaderboardLoading(false);
      } else {
        setLeaderboardLoading(false);
        console.error('Failed to fetch leaderboard');
      }
    } catch (err) {
      setLeaderboardLoading(false);
      console.error('Error fetching leaderboard:', err);
    }
  }, [token]);

  const fetchDashboardData = useCallback(async () => {
    if (!token) {
      return;
    }

    // Set loading states for all data fetches
    setOverviewLoading(true);
    setStatusDistributionLoading(true);
    setTrendsLoading(true);
    setCertificateTrendsLoading(true);
    setError(null);

    try {
      // Fetch all dashboard data in parallel
      const [overviewRes, statusRes, trendsRes, certificateTrendsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/analytics/dashboard/overview`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch(`${API_BASE_URL}/analytics/customers/status-distribution`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch(`${API_BASE_URL}/analytics/customers/trends?days=2000&granularity=monthly`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
        fetch(`${API_BASE_URL}/analytics/customers/trends-by-certificate-type?days=2000`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }),
      ]);

      // Process overview data
      if (overviewRes.ok) {
        const overviewData = await overviewRes.json();
        setOverview(overviewData);
        setOverviewLoading(false);
      } else {
        setOverviewLoading(false);
        throw new Error('Failed to fetch overview data');
      }

      // Process status distribution data
      if (statusRes.ok) {
        const statusData = await statusRes.json();
        setStatusDistribution(statusData);
        setStatusDistributionLoading(false);
      } else {
        setStatusDistributionLoading(false);
        throw new Error('Failed to fetch status distribution');
      }

      // Process trends data
      if (trendsRes.ok) {
        const trendsData = await trendsRes.json();
        setTrends(trendsData);
        setTrendsLoading(false);
      } else {
        setTrendsLoading(false);
        throw new Error('Failed to fetch trends');
      }

      // Handle certificate type trends separately (can fail gracefully)
      if (certificateTrendsRes.ok) {
        const certificateTrendsData = await certificateTrendsRes.json();
        setCertificateTrends(certificateTrendsData);
        setCertificateTrendsLoading(false);
      } else {
        setCertificateTrendsLoading(false);
        console.warn('Failed to fetch certificate type trends');
      }
    } catch (err) {
      // Set all loading states to false on error
      setOverviewLoading(false);
      setStatusDistributionLoading(false);
      setTrendsLoading(false);
      setCertificateTrendsLoading(false);
      setError(err instanceof Error ? err.message : 'An error occurred');
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

  // Save filters to localStorage whenever they change
  useEffect(() => {
    const filters = {
      selectedYear,
      selectedMonth,
      trendsViewOption,
      certificateTypes,
    };
    saveFiltersToStorage(filters);
  }, [selectedYear, selectedMonth, trendsViewOption, certificateTypes, saveFiltersToStorage]);

  // Clear filters from storage when user logs out
  useEffect(() => {
    if (!user && !token) {
      clearFiltersFromStorage();
    }
  }, [user, token, clearFiltersFromStorage]);

  // Fetch leaderboard when month/year changes
  useEffect(() => {
    if (token) {
      fetchLeaderboardByMonth(selectedYear, selectedMonth);
    }
  }, [token, selectedYear, selectedMonth, fetchLeaderboardByMonth]);


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
            loading={overviewLoading}
          />

          <MetricCard
            title={t('dashboard.metrics.newCustomers30d')}
            value={overview?.newCustomersThisPeriod || 0}
            change={overview?.periodChange.newCustomersChange}
            description={t('dashboard.metrics.fromLastPeriod')}
            loading={overviewLoading}
          />

          <MetricCard
            title={t('dashboard.metrics.activeCustomers')}
            value={overview?.activeCustomers || 0}
            description={t('dashboard.metrics.recentActivity')}
            loading={overviewLoading}
          />

          <MetricCard
            title={t('dashboard.metrics.conversionRate')}
            value={overview?.conversionRate ? `${overview.conversionRate.toFixed(1)}%` : '0%'}
            change={overview?.periodChange.conversionRateChange}
            description={t('dashboard.metrics.fromLastPeriod')}
            loading={overviewLoading}
          />
        </div>

        {/* Charts Section */}
        <div className="mt-8 space-y-8">
          {/* Customer Trends Chart */}
          <TrendLineChart
            data={trends?.dataPoints || []}
            title={t('dashboard.charts.trends')}
            granularity={trends?.granularity || 'daily'}
            loading={trendsLoading}
            error={error}
            viewOption={trendsViewOption}
            onViewOptionChange={setTrendsViewOption}
          />

          {/* Certificate Type Trends Chart */}
          <CertificateTypeTrendsChart
            data={certificateTrends || { trendsByCertificateType: {}, totalDays: 30 }}
            title={t('dashboard.charts.certificateTypeTrends')}
            loading={certificateTrendsLoading}
            error={error}
            selectedTypes={certificateTypes}
            onSelectedTypesChange={setCertificateTypes}
          />

          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Status Distribution Chart */}
            <StatusDistributionChart
              data={statusDistribution?.statusCounts || {}}
              totalCustomers={statusDistribution?.totalCustomers || 0}
              title={t('dashboard.charts.statusDistribution')}
              loading={statusDistributionLoading}
              error={error}
            />

            {/* Sales Leaderboard */}
            <div className="overflow-hidden rounded-lg bg-white shadow flex flex-col">
              <div className="p-6 flex-shrink-0 flex items-center justify-between">
                <h3 className="text-lg font-medium leading-6 text-gray-900">{t('dashboard.charts.leaderboard')}</h3>
                <div className="flex items-center gap-2">
                  {/* Year Selector */}
                  <select
                    value={selectedYear}
                    onChange={(e) => setSelectedYear(parseInt(e.target.value))}
                    className="text-sm border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    {Array.from({ length: 10 }, (_, i) => {
                      const year = new Date().getFullYear() - i;
                      return (
                        <option key={year} value={year}>
                          {year}
                        </option>
                      );
                    })}
                  </select>

                  {/* Month Selector */}
                  <select
                    value={selectedMonth === null ? 'all' : selectedMonth}
                    onChange={(e) => setSelectedMonth(e.target.value === 'all' ? null : parseInt(e.target.value))}
                    className="text-sm border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="all">{t('dashboard.charts.allMonths')}</option>
                    {Array.from({ length: 12 }, (_, i) => {
                      const monthKeys = ['january', 'february', 'march', 'april', 'may', 'june', 'july', 'august', 'september', 'october', 'november', 'december'];
                      const monthName = t(`dashboard.charts.months.${monthKeys[i]}`);
                      return (
                        <option key={i} value={i}>
                          {monthName}
                        </option>
                      );
                    })}
                  </select>
                </div>
              </div>
              <div className="flex-1 px-6 pb-6">
                {leaderboardLoading ? (
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
                            <p className="text-sm text-gray-500">{entry.conversionRate.toFixed(1)}%</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center text-center text-gray-500">
                    <p>{t('dashboard.charts.noLeaderboardData')}</p>
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