'use client';

import { useEffect, useRef, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
  TimeScale,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { useLanguage } from '@/contexts/LanguageContext';
import 'chartjs-adapter-date-fns';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

interface TrendDataPoint {
  date: string;
  newCustomers: number;
  totalCustomers: number;
  conversionRate: number;
}

interface CertificateTypeTrendsData {
  trendsByCertificateType: Record<string, TrendDataPoint[]>;
  totalDays: number;
}

interface CertificateTypeTrendsChartProps {
  data: CertificateTypeTrendsData;
  title?: string;
  className?: string;
  loading?: boolean;
  error?: string | null;
}

// Color scheme for different certificate types
const CERTIFICATE_TYPE_COLORS: Record<string, { border: string; background: string }> = {
  'BASIC': {
    border: 'rgb(99, 102, 241)',     // indigo-500
    background: 'rgba(99, 102, 241, 0.1)',
  },
  'STANDARD': {
    border: 'rgb(34, 197, 94)',      // green-500
    background: 'rgba(34, 197, 94, 0.1)',
  },
  'PREMIUM': {
    border: 'rgb(234, 179, 8)',      // yellow-500
    background: 'rgba(234, 179, 8, 0.1)',
  },
  'ENTERPRISE': {
    border: 'rgb(239, 68, 68)',      // red-500
    background: 'rgba(239, 68, 68, 0.1)',
  },
  'VIP': {
    border: 'rgb(168, 85, 247)',     // purple-500
    background: 'rgba(168, 85, 247, 0.1)',
  },
  'DEFAULT': {
    border: 'rgb(107, 114, 128)',    // gray-500
    background: 'rgba(107, 114, 128, 0.1)',
  },
};

export default function CertificateTypeTrendsChart({
  data,
  title,
  className = "",
  loading = false,
  error = null
}: CertificateTypeTrendsChartProps) {
  const { t } = useLanguage();
  const chartRef = useRef<ChartJS<'line'>>(null);
  const [hiddenTypes, setHiddenTypes] = useState<Set<string>>(new Set());

  // Use default title if not provided
  const chartTitle = title || t('dashboard.charts.certificateTypeTrends');

  // Get all unique dates across all certificate types
  const allDates = Array.from(
    new Set(
      Object.values(data.trendsByCertificateType).flat().map(point => point.date)
    )
  ).sort();

  // Prepare chart data with multiple datasets
  const certificateTypes = Object.keys(data.trendsByCertificateType).sort();

  const chartData = {
    labels: allDates.map(date => new Date(date)),
    datasets: certificateTypes.map((type, index) => {
      const colors = CERTIFICATE_TYPE_COLORS[type] || CERTIFICATE_TYPE_COLORS['DEFAULT'];
      const typeData = data.trendsByCertificateType[type] || [];

      return {
        label: type,
        data: allDates.map(date => {
          const dataPoint = typeData.find(point => point.date === date);
          return dataPoint ? dataPoint.newCustomers : 0;
        }),
        borderColor: colors!.border,
        backgroundColor: colors!.background,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBorderWidth: 2,
        pointBackgroundColor: 'white',
        fill: false,
        hidden: hiddenTypes.has(type),
      };
    }),
  };

  const chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      legend: {
        display: true,
        position: 'top' as const,
        labels: {
          font: {
            size: 12,
            family: 'Inter, sans-serif',
          },
          color: 'rgb(31, 41, 55)',
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 15,
        },
        onClick: (_event, legendItem, legend) => {
          const index = legendItem.datasetIndex;
          if (index !== undefined && certificateTypes[index]) {
            const type = certificateTypes[index]!;
            setHiddenTypes(prev => {
              const newHidden = new Set(prev);
              if (newHidden.has(type)) {
                newHidden.delete(type);
              } else {
                newHidden.add(type);
              }
              return newHidden;
            });
          }
        },
      },
      title: {
        display: !!chartTitle,
        text: chartTitle,
        font: {
          size: 16,
          weight: 'bold',
          family: 'Inter, sans-serif',
        },
        color: 'rgb(31, 41, 55)',
        padding: {
          bottom: 20,
        },
      },
      tooltip: {
        backgroundColor: 'rgba(31, 41, 55, 0.9)',
        titleFont: {
          size: 13,
          weight: 'bold',
        },
        bodyFont: {
          size: 12,
        },
        padding: 12,
        cornerRadius: 6,
        callbacks: {
          title: function(tooltipItems) {
            if (tooltipItems.length > 0) {
              const item = tooltipItems[0];
              if (item && typeof item.parsed.x === 'number') {
                const date = new Date(item.parsed.x);
                return date.toLocaleDateString('en-US', {
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
            const value = context.parsed.y;
            if (typeof value === 'number') {
              label += value.toLocaleString() + ' ' + t('dashboard.charts.newCertifications');
            } else {
              label += 'N/A';
            }
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
          color: 'rgb(107, 114, 128)', // gray-500
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
          text: t('dashboard.charts.newCertifications'),
          color: 'rgb(107, 114, 128)',
        },
      },
    },
  };

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="h-64 bg-gray-200 rounded"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="text-center text-red-500">
          <p>{t('dashboard.charts.error')}</p>
          <p className="text-sm text-gray-500 mt-1">{error}</p>
        </div>
      </div>
    );
  }

  if (!data || Object.keys(data.trendsByCertificateType).length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="text-center text-gray-500">
          <p>{t('dashboard.charts.noData')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-medium text-gray-900">{chartTitle}</h3>
        <div className="text-sm text-gray-500">
          {t('dashboard.charts.lastDays', { days: data.totalDays })}
        </div>
      </div>

      <div style={{ height: '350px' }}>
        <Line ref={chartRef} data={chartData} options={chartOptions} />
      </div>

      {/* Summary stats */}
      <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-gray-200">
        {certificateTypes.slice(0, 4).map(type => {
          const typeData = data.trendsByCertificateType[type] || [];
          const latestData = typeData.length > 0 ? typeData[typeData.length - 1] : undefined;
          const total = typeData.reduce((sum, point) => sum + point.newCustomers, 0);
          const colors = CERTIFICATE_TYPE_COLORS[type] || CERTIFICATE_TYPE_COLORS['DEFAULT'];

          return (
            <div key={type} className="text-center">
              <div
                className="text-lg font-semibold"
                style={{ color: colors!.border }}
              >
                {latestData?.newCustomers.toLocaleString() || '0'}
              </div>
              <div className="text-xs text-gray-500">{type}</div>
              <div className="text-xs text-gray-400">
                {t('dashboard.charts.total')}: {total.toLocaleString()}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}