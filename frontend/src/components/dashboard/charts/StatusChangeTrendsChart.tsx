'use client';

import React from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
  ChartDataset,
  TooltipItem,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import { useLanguage } from '@/contexts/LanguageContext';

// Status type constants mapped to translation keys and colors
const STATUS_TYPES = {
  NOTIFIED: {
    translationKey: 'status.notified',
    backgroundColor: 'rgba(59, 130, 246, 0.8)', // blue-500
    borderColor: 'rgb(59, 130, 246)', // blue-500
  },
  SUBMITTED: {
    translationKey: 'status.submitted',
    backgroundColor: 'rgba(34, 197, 94, 0.8)', // green-500
    borderColor: 'rgb(34, 197, 94)', // green-500
  },
  ABORTED: {
    translationKey: 'status.aborted',
    backgroundColor: 'rgba(239, 68, 68, 0.8)', // red-500
    borderColor: 'rgb(239, 68, 68)', // red-500
  },
  CERTIFIED_ELSEWHERE: {
    translationKey: 'status.certifiedElsewhere',
    backgroundColor: 'rgba(168, 85, 247, 0.8)', // purple-500
    borderColor: 'rgb(168, 85, 247)', // purple-500
  },
} as const;

type StatusType = keyof typeof STATUS_TYPES;

// Extended dataset type to include custom legendItem property
interface ExtendedChartDataset extends ChartDataset<'bar'> {
  legendItem?: boolean;
}

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

interface StatusChangeDataPoint {
  [key: string]: string | number; // Can be string (date) or number (counts)
}

interface StatusChangeTrendsResponse {
  dataPoints: StatusChangeDataPoint[];
  users: string[];
  totalDays: number;
}

interface StatusChangeTrendsChartProps {
  data?: StatusChangeTrendsResponse;
  title?: string;
  days?: number;
  className?: string;
  loading?: boolean;
  error?: string | null;
  onDaysChange?: (days: number) => void;
}

export default function StatusChangeTrendsChart({
  data,
  title,
  days = 7,
  className = "",
  loading = false,
  error = null,
  onDaysChange,
}: StatusChangeTrendsChartProps) {
  const { t, language } = useLanguage();

  const chartTitle = title || t('dashboard.charts.statusChangeTrends');
  const statusKeys = Object.keys(STATUS_TYPES) as StatusType[];
  const dataPoints = data?.dataPoints || [];
  const users = data?.users || [];

  // Extract dates from data points and format them
  const dates = dataPoints
    .map(dp => dp.date as string)
    .filter(d => d)
    .sort();

  // Format dates for display based on language
  const formattedDates = dates.map(dateStr => {
    const date = new Date(dateStr);
    // Map language codes to locale codes
    const locale = language === 'zh-CN' ? 'zh-CN' : 'en-US';
    return date.toLocaleDateString(locale, {
      month: 'short',
      day: 'numeric',
    });
  });

  // Prepare chart data - grouped bars by user for each date, stacked by status
  // Each user gets their own stack, with status types stacked within each user's bar
  const chartData = {
    labels: formattedDates, // Use formatted dates for display
    datasets: users.flatMap((user) =>
      statusKeys.map((statusKey) => ({
        label: user, // Show only user name in legend
        data: dates.map(date => {
          const dataPoint = dataPoints.find(dp => dp.date === date);
          if (!dataPoint) {return 0;}
          const key = `${user}_${statusKey}`;
          return dataPoint[key] || 0;
        }),
        backgroundColor: STATUS_TYPES[statusKey].backgroundColor,
        borderColor: STATUS_TYPES[statusKey].borderColor,
        borderWidth: 1,
        borderRadius: 4,
        barPercentage: 0.7,
        stack: user, // Each user gets their own stack
        // Use a custom property to filter legend items
        hidden: false,
        // Mark the first dataset for each user as the legend representative
        legendItem: statusKey === statusKeys[0],
      }))
    ),
  };

  // Chart options
  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'nearest' as const,
      axis: 'x' as const,
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
          color: 'white',
          // Generate custom legend items with white color and gray border
          generateLabels: function(chart) {
            const uniqueUsers: string[] = [];
            const userIndices: Map<string, number> = new Map();

            // Find unique users and their first dataset index
            chart.data.datasets.forEach((dataset, index) => {
              const extendedDataset = dataset as ExtendedChartDataset;
              if (extendedDataset.legendItem === true && dataset.label && !uniqueUsers.includes(dataset.label)) {
                uniqueUsers.push(dataset.label);
                userIndices.set(dataset.label, index);
              }
            });

            // Generate legend items for each user
            return uniqueUsers.map((user) => {
              const datasetIndex = userIndices.get(user) || 0;
              return {
                text: user,
                fillStyle: 'white',
                strokeStyle: 'rgb(209, 213, 219)', // gray-300 (light gray)
                lineWidth: 1,
                hidden: false,
                index: datasetIndex,
                datasetIndex: datasetIndex,
              };
            });
          },
        },
        onClick: (e, legendItem, legend) => {
          // Get the chart instance
          const chart = legend.chart;
          if (!chart) {return;}

          // Get the user name from the clicked legend item
          const user = legendItem.text;
          if (!user) {return;}

          // Find the first dataset index for this user
          const clickedIndex = chart.data.datasets.findIndex(
            ds => ds.label === user
          );
          if (clickedIndex === -1) {return;}

          // Get the target state (toggle to opposite of current state)
          const clickedMeta = chart.getDatasetMeta(clickedIndex);
          if (!clickedMeta) {return;}
          const targetState = !clickedMeta.hidden; // Toggle to opposite state

          chart.data.datasets.forEach((dataset, index) => {
            if (dataset.label === user) {
              // Toggle all datasets for this user
              const meta = chart.getDatasetMeta(index);
              if (meta) {
                meta.hidden = targetState;
              }
            }
          });

          chart.update();
        },
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        filter: function(tooltipItem) {
          // Filter to show only datasets for the hovered user
          // Get the user from the hovered dataset
          const hoveredStack = tooltipItem.dataset.stack;
          // Show all items that belong to the same stack (same user)
          return tooltipItem.dataset.stack === hoveredStack;
        },
        callbacks: {
          title: function(tooltipItems: TooltipItem<"bar">[]) {
            // Show the date and user as title
            const index = tooltipItems[0]?.dataIndex;
            const user = tooltipItems[0]?.dataset.label;
            if (index === undefined || !user) {return '';}
            return `${formattedDates[index]} - ${user}`;
          },
          label: function(context: TooltipItem<"bar">) {
            const dataset = context.dataset;
            const dataIndex = context.dataIndex;
            const value = dataset.data[dataIndex];

            // Get the status type from the background color
            const statusType = statusKeys.find(key => {
              return STATUS_TYPES[key].backgroundColor === dataset.backgroundColor;
            });

            if (!statusType || !value || value === 0) {return '';} // Skip zero values

            const statusLabel = t(STATUS_TYPES[statusType].translationKey);
            return `${statusLabel}: ${value.toLocaleString()}`;
          },
        },
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          font: {
            size: 11,
            family: 'Inter, sans-serif',
          },
          color: 'rgb(107, 114, 128)',
          maxTicksLimit: 10,
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
        beginAtZero: true,
      },
    },
    elements: {
      bar: {
        borderRadius: 4,
      },
    },
  };

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="h-5 bg-gray-200 rounded w-48 mb-4"></div>
          <div className="h-80 bg-gray-200 rounded mb-4"></div>
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

  if (!data || !data.dataPoints || dataPoints.length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{chartTitle}</h3>
        <div className="flex items-center justify-center h-80 text-gray-500">
          <p>{t('dashboard.charts.noData')}</p>
        </div>
      </div>
    );
  }

  // Calculate total counts for each user across all dates and all statuses
  const userTotals = users.map(user => {
    return dates.reduce((sum, date) => {
      const dataPoint = dataPoints.find(dp => dp.date === date);
      if (!dataPoint) {return sum;}

      let userTotal = 0;
      statusKeys.forEach(statusKey => {
        const key = `${user}_${statusKey}`;
        const value = dataPoint[key];
        userTotal += typeof value === 'number' ? value : 0;
      });
      return sum + userTotal;
    }, 0);
  });

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-500">{t('dashboard.charts.last')}</span>
          <select
            value={days}
            onChange={(e) => onDaysChange?.(parseInt(e.target.value))}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value={7}>7</option>
            <option value={30}>30</option>
            <option value={90}>90</option>
            <option value={180}>180</option>
            <option value={365}>365</option>
          </select>
          <span className="text-sm text-gray-500">{t('dashboard.charts.days')}</span>
        </div>
      </div>

      <div className="relative h-80">
        <Bar
          data={chartData}
          options={options}
        />
      </div>

      {/* Summary stats - show total for each user */}
      <div className="mt-4 grid grid-cols-4 gap-4 pt-4 border-t border-gray-200">
        {users.slice(0, 4).map((user, index) => (
          <div key={user} className="text-center">
            <div className="text-lg font-semibold text-gray-800">
              {(userTotals[index] || 0).toLocaleString()}
            </div>
            <div className="text-xs text-gray-500">{user}</div>
            <div className="text-xs text-gray-400">{t('dashboard.charts.totalLabel')}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
