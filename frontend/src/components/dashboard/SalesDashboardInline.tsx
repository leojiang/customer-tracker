'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import MetricCard from '@/components/dashboard/widgets/MetricCard';
import { getTranslatedStatusName } from '@/types/customer';

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

interface SalesDashboardInlineProps {
  onNavigateToCustomers?: () => void;
}

/**
 * Sales Dashboard Inline Component - renders without navigation for embedding in main page
 */
export default function SalesDashboardInline({ onNavigateToCustomers }: SalesDashboardInlineProps = {}) {
  const { token } = useAuth();
  const { t } = useLanguage();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution | null>(null);
  const [performance, setPerformance] = useState<SalesPerformance | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

  const fetchDashboardData = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Fetch all dashboard data in parallel
      const [overviewRes, statusRes, performanceRes] = await Promise.all([
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
        fetch(`${API_BASE_URL}/analytics/sales/performance`, {
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
    }
  }, [token]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (loading) {
    return (
      <div className="bg-gray-50 rounded-lg p-8">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">{t('dashboard.metrics.loadingDashboard')}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-gray-50 rounded-lg p-8">
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
      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title={t('dashboard.metrics.myCustomers')}
          value={overview?.totalCustomers || 0}
          change={overview?.periodChange.totalCustomersChange}
          description={t('dashboard.metrics.fromLastPeriod')}
          loading={loading}
        />
        
        <MetricCard
          title={t('dashboard.metrics.newCustomers')}
          value={overview?.newCustomersThisPeriod || 0}
          change={overview?.periodChange.newCustomersChange}
          description={t('dashboard.metrics.fromLastPeriod')}
          loading={loading}
        />
        
        <MetricCard
          title={t('dashboard.metrics.activeCustomers')}
          value={overview?.activeCustomers || 0}
          description={t('dashboard.metrics.recentActivity')}
          loading={loading}
        />
        
        <MetricCard
          title={t('dashboard.sales.myConversionRate')}
          value={overview?.conversionRate ? `${overview.conversionRate.toFixed(1)}%` : '0%'}
          change={overview?.periodChange.conversionRateChange}
          description={t('dashboard.metrics.fromLastPeriod')}
          loading={loading}
        />
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        {/* My Pipeline Status */}
        {statusDistribution && (
          <div className="overflow-hidden rounded-lg bg-white shadow">
            <div className="p-6">
              <h3 className="text-lg font-medium leading-6 text-gray-900">{t('dashboard.charts.myPipeline')}</h3>
              <div className="mt-6">
                {Object.entries(statusDistribution.statusCounts).map(([status, count]) => (
                  <div key={status} className="flex items-center justify-between py-3 border-b border-gray-200 last:border-b-0">
                    <span className="text-sm font-medium text-gray-700">
                      {getTranslatedStatusName(status, t)}
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
              <h3 className="text-lg font-medium leading-6 text-gray-900">{t('dashboard.charts.performanceSummary')}</h3>
              <div className="mt-6 space-y-6">
                <div>
                  <div className="flex justify-between text-sm">
                    <span className="font-medium text-gray-700">{t('dashboard.sales.conversionProgress')}</span>
                    <span className="text-gray-500">{performance.conversions} / {performance.totalCustomers}</span>
                  </div>
                  <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
                    <div 
                      className="bg-green-600 h-2 rounded-full" 
                      style={{ width: `${performance.conversionRate}%` }}
                    ></div>
                  </div>
                  <p className="mt-1 text-sm text-gray-500">
                    {performance.conversionRate.toFixed(1)}% {t('dashboard.sales.conversionRate')}
                  </p>
                </div>

                <div className="border-t border-gray-200 pt-6">
                  <h4 className="text-sm font-medium text-gray-700 mb-4">{t('dashboard.sales.keyMetrics')}</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center">
                      <p className="text-2xl font-semibold text-gray-900">{performance.newCustomers}</p>
                      <p className="text-xs text-gray-500">{t('dashboard.sales.newCustomers')}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-2xl font-semibold text-gray-900">{performance.conversions}</p>
                      <p className="text-xs text-gray-500">{t('dashboard.sales.conversions')}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="rounded-lg bg-white shadow p-6">
        <h3 className="text-lg font-medium leading-6 text-gray-900 mb-4">{t('dashboard.sales.quickActions')}</h3>
        <div className="flex flex-wrap gap-3">
          <button 
            onClick={onNavigateToCustomers}
            className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
          >
            {t('dashboard.sales.viewAllCustomers')}
          </button>
          <button 
            onClick={onNavigateToCustomers}
            className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
          >
            {t('dashboard.sales.followUpCalls')}
          </button>
          <button 
            onClick={onNavigateToCustomers}
            className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
          >
            {t('dashboard.sales.pendingOrders')}
          </button>
        </div>
      </div>
    </div>
  );
}