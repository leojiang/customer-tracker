'use client';

import { useEffect, useRef } from 'react';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  ChartOptions
} from 'chart.js';
import { Doughnut } from 'react-chartjs-2';

// Register Chart.js components
ChartJS.register(ArcElement, Tooltip, Legend);

interface StatusDistributionChartProps {
  data: Record<string, number>;
  totalCustomers: number;
  title?: string;
  className?: string;
  loading?: boolean;
  error?: string | null;
}

export default function StatusDistributionChart({ 
  data, 
  totalCustomers, 
  title = "Customer Status Distribution",
  className = "",
  loading = false,
  error = null
}: StatusDistributionChartProps) {
  const chartRef = useRef<ChartJS<'doughnut'>>(null);

  // Transform status names for better display
  const formatStatusName = (status: string): string => {
    return status
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  };

  // Generate chart data
  const chartData = {
    labels: Object.keys(data).map(formatStatusName),
    datasets: [
      {
        label: 'Customers',
        data: Object.values(data),
        backgroundColor: [
          'rgb(99, 102, 241)',   // indigo-500
          'rgb(34, 197, 94)',    // green-500
          'rgb(234, 179, 8)',    // yellow-500
          'rgb(239, 68, 68)',    // red-500
          'rgb(168, 85, 247)',   // purple-500
          'rgb(236, 72, 153)',   // pink-500
          'rgb(14, 165, 233)',   // sky-500
        ],
        borderColor: [
          'rgb(79, 70, 229)',    // indigo-600
          'rgb(22, 163, 74)',    // green-600
          'rgb(202, 138, 4)',    // yellow-600
          'rgb(220, 38, 38)',    // red-600
          'rgb(147, 51, 234)',   // purple-600
          'rgb(219, 39, 119)',   // pink-600
          'rgb(2, 132, 199)',    // sky-600
        ],
        borderWidth: 2,
        hoverBackgroundColor: [
          'rgb(79, 70, 229)',
          'rgb(22, 163, 74)',
          'rgb(202, 138, 4)',
          'rgb(220, 38, 38)',
          'rgb(147, 51, 234)',
          'rgb(219, 39, 119)',
          'rgb(2, 132, 199)',
        ],
      },
    ],
  };

  // Chart options
  const options: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right' as const,
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
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.parsed;
            const percentage = totalCustomers > 0 ? ((value / totalCustomers) * 100).toFixed(1) : '0';
            return `${label}: ${value} (${percentage}%)`;
          },
        },
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
      },
    },
    cutout: '60%', // Makes it a donut chart
    elements: {
      arc: {
        borderWidth: 2,
      },
    },
  };

  // Clean up chart on unmount
  useEffect(() => {
    return () => {
      const chart = chartRef.current;
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
          <div className="h-64 bg-gray-200 rounded mb-4"></div>
          <div className="grid grid-cols-2 gap-4">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="text-center">
                <div className="h-6 bg-gray-200 rounded w-12 mx-auto mb-1"></div>
                <div className="h-4 bg-gray-200 rounded w-16 mx-auto"></div>
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
        <h3 className="text-lg font-medium text-gray-900 mb-4">{title}</h3>
        <div className="flex items-center justify-center h-64 text-red-500">
          <div className="text-center">
            <p className="mb-2">Error loading chart</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!data || Object.keys(data).length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{title}</h3>
        <div className="flex items-center justify-center h-64 text-gray-500">
          <p>No data available</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <span className="text-sm text-gray-500">
          Total: {totalCustomers.toLocaleString()} customers
        </span>
      </div>
      
      <div className="relative h-64">
        <Doughnut 
          ref={chartRef}
          data={chartData} 
          options={options} 
        />
        
        {/* Center label showing total */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">
              {totalCustomers.toLocaleString()}
            </div>
            <div className="text-sm text-gray-500">
              Total Customers
            </div>
          </div>
        </div>
      </div>

      {/* Summary statistics */}
      <div className="mt-4 grid grid-cols-2 gap-4">
        {Object.entries(data).slice(0, 4).map(([status, count]) => (
          <div key={status} className="text-center">
            <div className="text-lg font-semibold text-gray-900">
              {count.toLocaleString()}
            </div>
            <div className="text-xs text-gray-500">
              {formatStatusName(status)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}