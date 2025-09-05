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
        display: false, // Hide default legend - we'll create a custom one
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
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.2)',
        borderWidth: 1,
        cornerRadius: 8,
        titleFont: {
          size: 14,
          weight: '600',
        },
        bodyFont: {
          size: 13,
        },
        padding: 12,
      },
    },
    cutout: '65%', // Makes it a donut chart with more space for center content
    elements: {
      arc: {
        borderWidth: 3,
        borderColor: '#ffffff',
      },
    },
    animation: {
      animateRotate: true,
      animateScale: true,
      duration: 1000,
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
      <div className={`bg-white rounded-xl shadow-lg border border-gray-100 p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="flex justify-between items-center mb-6">
            <div className="h-6 bg-gray-200 rounded w-48"></div>
            <div className="h-6 bg-gray-100 rounded-full w-20"></div>
          </div>
          
          <div className="flex flex-col lg:flex-row items-center gap-8">
            <div className="w-80 h-80 bg-gray-200 rounded-full mx-auto lg:mx-0"></div>
            
            <div className="flex-1 space-y-4 w-full">
              {[1, 2, 3, 4, 5].map(i => (
                <div key={i} className="p-3 rounded-lg bg-gray-50">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center">
                      <div className="w-4 h-4 bg-gray-300 rounded-full mr-3"></div>
                      <div>
                        <div className="h-4 bg-gray-200 rounded w-24 mb-1"></div>
                        <div className="h-3 bg-gray-200 rounded w-16"></div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="h-5 bg-gray-200 rounded w-12 mb-1"></div>
                      <div className="h-3 bg-gray-200 rounded w-16"></div>
                    </div>
                  </div>
                  <div className="h-1.5 bg-gray-200 rounded-full"></div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`bg-white rounded-xl shadow-lg border border-gray-100 p-6 ${className}`}>
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          <div className="px-3 py-1 bg-red-100 rounded-full">
            <span className="text-sm font-medium text-red-700">Error</span>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center h-80 text-red-500 bg-red-50 rounded-xl border-2 border-dashed border-red-200">
          <div className="text-center">
            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
              <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p className="text-lg font-semibold text-red-700 mb-2">Error loading chart</p>
            <p className="text-sm text-red-600 max-w-md">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!data || Object.keys(data).length === 0) {
    return (
      <div className={`bg-white rounded-xl shadow-lg border border-gray-100 p-6 ${className}`}>
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          <div className="px-3 py-1 bg-gray-100 rounded-full">
            <span className="text-sm font-medium text-gray-700">No data</span>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center h-80 text-gray-500 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
          <div className="text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <p className="text-lg font-semibold text-gray-600 mb-2">No data available</p>
            <p className="text-sm text-gray-500">Customer status data will appear here when available</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-xl shadow-lg border border-gray-100 p-6 ${className}`}>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
        <div className="px-3 py-1 bg-gray-100 rounded-full">
          <span className="text-sm font-medium text-gray-700">
            {totalCustomers.toLocaleString()} total
          </span>
        </div>
      </div>
      
      <div className="flex flex-col lg:flex-row items-center gap-8">
        {/* Chart Container */}
        <div className="relative w-80 h-80 mx-auto lg:mx-0">
          <Doughnut 
            ref={chartRef}
            data={chartData} 
            options={options} 
          />
          
          {/* Perfectly Centered Content */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="text-center">
              <div className="text-4xl font-bold text-gray-900 mb-1">
                {totalCustomers.toLocaleString()}
              </div>
              <div className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total Customers
              </div>
              <div className="mt-2 h-px w-16 bg-gradient-to-r from-indigo-500 to-purple-500 mx-auto"></div>
            </div>
          </div>
        </div>

        {/* Custom Legend */}
        <div className="flex-1 min-w-0">
          <div className="space-y-4">
            {Object.entries(data).map(([status, count], index) => {
              const percentage = totalCustomers > 0 ? ((count / totalCustomers) * 100) : 0;
              const color = chartData.datasets[0].backgroundColor[index];
              
              return (
                <div key={status} className="group hover:bg-gray-50 rounded-lg p-3 transition-all duration-200 cursor-pointer">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center min-w-0 flex-1">
                      <div 
                        className="w-4 h-4 rounded-full mr-3 ring-2 ring-white shadow-sm group-hover:scale-110 transition-transform"
                        style={{ backgroundColor: color }}
                      ></div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {formatStatusName(status)}
                        </p>
                        <p className="text-xs text-gray-500">
                          {percentage.toFixed(1)}% of total
                        </p>
                      </div>
                    </div>
                    <div className="text-right ml-4">
                      <div className="text-lg font-semibold text-gray-900">
                        {count.toLocaleString()}
                      </div>
                      <div className="text-xs text-gray-500">
                        customers
                      </div>
                    </div>
                  </div>
                  
                  {/* Progress Bar */}
                  <div className="mt-2 w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700 ease-out"
                      style={{ 
                        backgroundColor: color,
                        width: `${percentage}%`,
                        boxShadow: `0 0 10px ${color}40`
                      }}
                    ></div>
                  </div>
                </div>
              );
            })}
          </div>
          
          {/* Summary Stats */}
          <div className="mt-6 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border border-blue-100">
            <div className="text-center">
              <div className="text-sm font-medium text-blue-900 mb-2">Quick Stats</div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-2xl font-bold text-blue-700">
                    {Object.values(data).reduce((max, current) => Math.max(max, current), 0)}
                  </div>
                  <div className="text-xs text-blue-600">Highest Count</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-blue-700">
                    {Object.keys(data).length}
                  </div>
                  <div className="text-xs text-blue-600">Status Types</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}