'use client';

import { useEffect, useRef, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
  TimeScale,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import { useLanguage } from '@/contexts/LanguageContext';
import 'chartjs-adapter-date-fns';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

interface StatusChangeDataPoint {
  date: string;
  NOTIFIED: number;
  SUBMITTED: number;
  ABORTED: number;
  CERTIFIED_ELSEWHERE: number;
}

interface StatusChangeTrendsResponse {
  dataPoints: StatusChangeDataPoint[];
  granularity: string;
  totalDays: number;
}

interface StatusChangeTrendsChartProps {
  data?: StatusChangeTrendsResponse;
  title?: string;
  days?: number;
  className?: string;
  loading?: boolean;
  error?: string | null;
}

export default function StatusChangeTrendsChart({
  data,
  title,
  days = 30,
  className = "",
  loading = false,
  error = null,
}: StatusChangeTrendsChartProps) {
  const { t, language } = useLanguage();
  const chartRef = useRef<ChartJS<'bar'>>(null);

  const chartTitle = title || t('dashboard.charts.statusChangeTrends');

  // Prepare chart data with 4 statuses
  const chartData = {
    labels: data?.dataPoints.map(point => new Date(point.date)) || [],
    datasets: [
      {
        label: t('customerStatus.NOTIFIED'),
        data: data?.dataPoints.map(point => point.NOTIFIED) || [],
        backgroundColor: 'rgba(59, 130, 246, 0.8)', // blue-500
        borderColor: 'rgb(59, 130, 246)', // blue-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.8,
        stack: 'statusChanges',
      },
      {
        label: t('customerStatus.SUBMITTED'),
        data: data?.dataPoints.map(point => point.SUBMITTED) || [],
        backgroundColor: 'rgba(34, 197, 94, 0.8)', // green-500
        borderColor: 'rgb(34, 197, 94)', // green-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.8,
        stack: 'statusChanges',
      },
      {
        label: t('customerStatus.ABORTED'),
        data: data?.dataPoints.map(point => point.ABORTED) || [],
        backgroundColor: 'rgba(239, 68, 68, 0.8)', // red-500
        borderColor: 'rgb(239, 68, 68)', // red-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.8,
        stack: 'statusChanges',
      },
      {
        label: t('customerStatus.CERTIFIED_ELSEWHERE'),
        data: data?.dataPoints.map(point => point.CERTIFIED_ELSEWHERE) || [],
        backgroundColor: 'rgba(168, 85, 247, 0.8)', // purple-500
        borderColor: 'rgb(168, 85, 247)', // purple-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.8,
        stack: 'statusChanges',
      },
    ],
  };

  // Chart options
  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      legend: {
        position: 'top' as const,
        labels: {
          usePointStyle: true,
          padding: 20,
          font: {
            size: 12,
            family: 'Inter, sans-serif',
          },
        },
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          title: function(tooltipItems) {
            if (tooltipItems.length > 0) {
              const item = tooltipItems[0];
              if (item && typeof item.parsed.x === 'number') {
                const date = new Date(item.parsed.x);
                const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';
                return date.toLocaleDateString(localeCode, {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric'
                });
              }
            }
            return '';
          },
          label: function(context) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed.y === null) {
              return label + 'N/A';
            }
            label += context.parsed.y.toLocaleString();
            return label;
          },
        },
      },
    },
    scales: {
      x: {
        type: 'time' as const,
        time: {
          unit: 'day',
          displayFormats: {
            day: 'MMM dd',
          },
        },
        grid: {
          display: false,
        },
        ticks: {
          font: {
            size: 11,
            family: 'Inter, sans-serif',
          },
          color: 'rgb(107, 114, 128)',
          callback: function(value) {
            const date = new Date(value);
            const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';
            return date.toLocaleDateString(localeCode, {
              month: 'short',
              day: 'numeric'
            });
          },
        },
      },
      y: {
        type: 'linear' as const,
        display: true,
        position: 'left' as const,
        grid: {
          color: 'rgba(107, 114, 128, 0.1)',
        },
        ticks: {
          font: {
            size: 11,
            family: 'Inter, sans-serif',
          },
          color: 'rgb(107, 114, 128)',
          callback: function(value) {
            return Number(value).toLocaleString();
          },
        },
        title: {
          display: true,
          text: t('dashboard.charts.statusChanges'),
          color: 'rgb(107, 114, 128)',
        },
        stacked: true,
      },
    },
    elements: {
      bar: {
        borderRadius: 4,
      },
    },
  };

  // Clean up chart on unmount
  useEffect(() => {
    const chart = chartRef.current;
    return () => {
      if (chart) {
        chart.destroy();
      }
    };
  }, []);

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="h-5 bg-gray-200 rounded w-48 mb-4"></div>
          <div className="h-80 bg-gray-200 rounded mb-4"></div>
          <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-200">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="text-center">
                <div className="h-6 bg-gray-200 rounded w-16 mx-auto mb-1"></div>
                <div className="h-4 bg-gray-200 rounded w-20 mx-auto"></div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{chartTitle}</h3>
        <div className="flex items-center justify-center h-80 text-red-500">
          <div className="text-center">
            <p className="mb-2">{t('dashboard.charts.errorLoading')}</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!data || !data.dataPoints || data.dataPoints.length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{chartTitle}</h3>
        <div className="flex items-center justify-center h-80 text-gray-500">
          <p>No status change data available</p>
        </div>
      </div>
    );
  }

  // Calculate summary statistics
  const latestData = data.dataPoints[data.dataPoints.length - 1];
  const totals = data.dataPoints.reduce((acc, point) => ({
    NOTIFIED: acc.NOTIFIED + point.NOTIFIED,
    SUBMITTED: acc.SUBMITTED + point.SUBMITTED,
    ABORTED: acc.ABORTED + point.ABORTED,
    CERTIFIED_ELSEWHERE: acc.CERTIFIED_ELSEWHERE + point.CERTIFIED_ELSEWHERE,
  }), { NOTIFIED: 0, SUBMITTED: 0, ABORTED: 0, CERTIFIED_ELSEWHERE: 0 });

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <div className="text-sm text-gray-500">
          {t('dashboard.charts.last')} {days} {t('dashboard.charts.days')}
        </div>
      </div>

      <div className="relative h-80">
        <Bar
          ref={chartRef}
          data={chartData}
          options={options}
        />
      </div>

      {/* Summary stats */}
      <div className="mt-4 grid grid-cols-4 gap-4 pt-4 border-t border-gray-200">
        <div className="text-center">
          <div className="text-lg font-semibold text-blue-600">
            {latestData.NOTIFIED.toLocaleString()}
          </div>
          <div className="text-xs text-gray-500">{t('customerStatus.NOTIFIED')}</div>
          <div className="text-xs text-gray-400">Total: {totals.NOTIFIED}</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-green-600">
            {latestData.SUBMITTED.toLocaleString()}
          </div>
          <div className="text-xs text-gray-500">{t('customerStatus.SUBMITTED')}</div>
          <div className="text-xs text-gray-400">Total: {totals.SUBMITTED}</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-red-600">
            {latestData.ABORTED.toLocaleString()}
          </div>
          <div className="text-xs text-gray-500">{t('customerStatus.ABORTED')}</div>
          <div className="text-xs text-gray-400">Total: {totals.ABORTED}</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-purple-600">
            {latestData.CERTIFIED_ELSEWHERE.toLocaleString()}
          </div>
          <div className="text-xs text-gray-500">{t('customerStatus.CERTIFIED_ELSEWHERE')}</div>
          <div className="text-xs text-gray-400">Total: {totals.CERTIFIED_ELSEWHERE}</div>
        </div>
      </div>
    </div>
  );
}