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

interface TrendDataPoint {
  date: string;
  newCustomers: number; // This now represents certifications based on certifiedAt
  totalCustomers: number;
  conversionRate: number;
}

interface TrendLineChartProps {
  data: TrendDataPoint[];
  title?: string;
  granularity?: string;
  days?: number;
  className?: string;
  loading?: boolean;
  error?: string | null;
  viewOption?: string;
  onViewOptionChange?: (option: string) => void;
}

export default function TrendLineChart({
  data,
  title,
  granularity = "daily",
  className = "",
  loading = false,
  error = null,
  viewOption,
  onViewOptionChange,
}: TrendLineChartProps) {
  const { t, language } = useLanguage();
  const chartRef = useRef<ChartJS<'bar'>>(null);

  // Use controlled prop if provided, otherwise use local state
  const [internalViewOption, setInternalViewOption] = useState('newCertifications');
  const activeDataset = viewOption !== undefined ? viewOption : internalViewOption;

  const handleViewOptionChange = (newValue: string) => {
    if (onViewOptionChange) {
      onViewOptionChange(newValue);
    } else {
      setInternalViewOption(newValue);
    }
  };

  // Use default title if not provided
  const chartTitle = title || t('dashboard.charts.trends');

  // Prepare chart data with both new certifications and total customers
  const chartData = {
    labels: data.map(point => new Date(point.date)),
    datasets: [
      {
        label: t('dashboard.charts.newCertifications'),
        data: data.map(point => point.newCustomers),
        backgroundColor: 'rgba(99, 102, 241, 0.8)', // indigo-500 with opacity
        borderColor: 'rgb(99, 102, 241)', // indigo-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.7,
        yAxisID: 'y',
        hidden: activeDataset !== 'newCertifications',
      },
      {
        label: t('dashboard.charts.totalCustomers'),
        data: data.map(point => point.totalCustomers),
        backgroundColor: 'rgba(34, 197, 94, 0.8)', // green-500 with opacity
        borderColor: 'rgb(34, 197, 94)', // green-500
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.7,
        yAxisID: 'y1',
        hidden: activeDataset !== 'totalCustomers',
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
        onClick: (event, legendItem, legend) => {
          // Custom legend click behavior - show/hide datasets
          const chart = legend.chart;

          // Toggle dataset visibility
          if (legendItem.datasetIndex !== undefined) {
            const meta = chart.getDatasetMeta(legendItem.datasetIndex);
            const dataset = chart.data.datasets[legendItem.datasetIndex];
            meta.hidden = meta.hidden === null ? !(dataset?.hidden ?? false) : !meta.hidden;
            chart.update();
          }
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

                // Map language to locale code
                const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';

                // Format based on granularity
                if (granularity === 'monthly') {
                  return date.toLocaleDateString(localeCode, {
                    year: 'numeric',
                    month: 'long'
                  });
                } else {
                  return date.toLocaleDateString(localeCode, {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                  });
                }
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
          unit: granularity === 'monthly' ? 'month' : (granularity === 'daily' ? 'day' : 'week'),
          displayFormats: {
            day: 'MMM dd',
            week: 'MMM dd',
            month: 'MMM yyyy',
          },
          // Add localized formatting for the axis
          tooltipFormat: granularity === 'monthly'
            ? 'MMM yyyy'
            : 'MMM dd, yyyy',
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
          // Use locale-aware date formatting for axis labels
          callback: function(value) {
            const date = new Date(value);
            const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';

            if (granularity === 'monthly') {
              return date.toLocaleDateString(localeCode, {
                year: 'numeric',
                month: 'short'
              });
            } else {
              return date.toLocaleDateString(localeCode, {
                month: 'short',
                day: 'numeric'
              });
            }
          },
        },
      },
      y: {
        type: 'linear' as const,
        display: activeDataset === 'newCertifications' || activeDataset === null,
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
      y1: {
        type: 'linear' as const,
        display: activeDataset === 'totalCustomers',
        position: 'right' as const,
        grid: {
          drawOnChartArea: false,
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
          text: t('dashboard.charts.totalCustomers'),
          color: 'rgb(107, 114, 128)',
        },
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
          <div className="flex justify-between items-center mb-4">
            <div className="h-5 bg-gray-200 rounded w-48"></div>
            <div className="h-8 bg-gray-200 rounded w-32"></div>
          </div>
          <div className="h-80 bg-gray-200 rounded mb-4"></div>
          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-200">
            {[1, 2].map(i => (
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

  if (!data || data.length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{chartTitle}</h3>
        <div className="flex items-center justify-center h-80 text-gray-500">
          <p>No trend data available</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-500">{t('dashboard.charts.view')}</span>
          <select
            value={activeDataset}
            onChange={(e) => handleViewOptionChange(e.target.value)}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value="newCertifications">{t('dashboard.charts.newCertifications')}</option>
            <option value="totalCustomers">{t('dashboard.charts.totalCustomers')}</option>
          </select>
        </div>
      </div>

      <div className="relative h-80">
        <Bar
          ref={chartRef}
          data={chartData}
          options={options}
        />
      </div>

      {/* Summary stats - both metrics */}
      <div className="mt-4 grid grid-cols-2 gap-4 pt-4 border-t border-gray-200">
        <div className="text-center">
          <div className="text-lg font-semibold text-indigo-600">
            {data[data.length - 1]?.newCustomers.toLocaleString() || '0'}
          </div>
          <div className="text-xs text-gray-500">{t('dashboard.charts.latestNew')}</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-green-600">
            {data[data.length - 1]?.totalCustomers.toLocaleString() || '0'}
          </div>
          <div className="text-xs text-gray-500">{t('dashboard.charts.totalNow')}</div>
        </div>
      </div>
    </div>
  );
}