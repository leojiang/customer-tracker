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

interface TrendLineChartProps {
  data: TrendDataPoint[];
  title?: string;
  granularity?: string;
  days?: number;
  className?: string;
  loading?: boolean;
  error?: string | null;
}

export default function TrendLineChart({ 
  data, 
  title = "Customer Growth Trends",
  granularity = "daily",
  days = 30,
  className = "",
  loading = false,
  error = null
}: TrendLineChartProps) {
  const chartRef = useRef<ChartJS<'line'>>(null);
  const [activeDataset, setActiveDataset] = useState('newCustomers');

  // Prepare chart data
  const chartData = {
    labels: data.map(point => new Date(point.date)),
    datasets: [
      {
        label: 'New Customers',
        data: data.map(point => point.newCustomers),
        borderColor: 'rgb(99, 102, 241)', // indigo-500
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBorderWidth: 2,
        pointBackgroundColor: 'white',
        fill: activeDataset === 'newCustomers',
        hidden: activeDataset !== 'newCustomers' && activeDataset !== 'all',
      },
      {
        label: 'Total Customers',
        data: data.map(point => point.totalCustomers),
        borderColor: 'rgb(34, 197, 94)', // green-500
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBorderWidth: 2,
        pointBackgroundColor: 'white',
        fill: activeDataset === 'totalCustomers',
        hidden: activeDataset !== 'totalCustomers' && activeDataset !== 'all',
        yAxisID: 'y1',
      },
      {
        label: 'Conversion Rate (%)',
        data: data.map(point => point.conversionRate),
        borderColor: 'rgb(234, 179, 8)', // yellow-500
        backgroundColor: 'rgba(234, 179, 8, 0.1)',
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBorderWidth: 2,
        pointBackgroundColor: 'white',
        fill: false,
        hidden: activeDataset !== 'conversionRate' && activeDataset !== 'all',
        yAxisID: 'y2',
      },
    ],
  };

  // Chart options
  const options: ChartOptions<'line'> = {
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
          const datasets = chart.data.datasets;
          
          // Toggle dataset visibility
          if (legendItem.datasetIndex !== undefined) {
            const meta = chart.getDatasetMeta(legendItem.datasetIndex);
            meta.hidden = meta.hidden === null ? !chart.data.datasets[legendItem.datasetIndex].hidden : null;
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
              const date = new Date(tooltipItems[0].parsed.x);
              return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
              });
            }
            return '';
          },
          label: function(context) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.datasetIndex === 2) {
              // Conversion rate
              label += context.parsed.y.toFixed(1) + '%';
            } else {
              label += context.parsed.y.toLocaleString();
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
          unit: granularity === 'daily' ? 'day' : 'week',
          displayFormats: {
            day: 'MMM dd',
            week: 'MMM dd',
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
        display: activeDataset === 'newCustomers' || activeDataset === 'all',
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
          text: 'New Customers',
          color: 'rgb(107, 114, 128)',
        },
      },
      y1: {
        type: 'linear' as const,
        display: activeDataset === 'totalCustomers' || activeDataset === 'all',
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
          text: 'Total Customers',
          color: 'rgb(107, 114, 128)',
        },
      },
      y2: {
        type: 'linear' as const,
        display: activeDataset === 'conversionRate' || activeDataset === 'all',
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
            return Number(value).toFixed(1) + '%';
          },
        },
        title: {
          display: true,
          text: 'Conversion Rate (%)',
          color: 'rgb(107, 114, 128)',
        },
      },
    },
    elements: {
      point: {
        hoverRadius: 8,
      },
      line: {
        borderWidth: 3,
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
          <div className="flex justify-between items-center mb-4">
            <div className="h-5 bg-gray-200 rounded w-48"></div>
            <div className="h-8 bg-gray-200 rounded w-32"></div>
          </div>
          <div className="h-80 bg-gray-200 rounded mb-4"></div>
          <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
            {[1, 2, 3].map(i => (
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
        <h3 className="text-lg font-medium text-gray-900 mb-4">{title}</h3>
        <div className="flex items-center justify-center h-80 text-red-500">
          <div className="text-center">
            <p className="mb-2">Error loading chart</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{title}</h3>
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
          <span className="text-sm text-gray-500">View:</span>
          <select 
            value={activeDataset}
            onChange={(e) => setActiveDataset(e.target.value)}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value="newCustomers">New Customers</option>
            <option value="totalCustomers">Total Customers</option>
            <option value="conversionRate">Conversion Rate</option>
            <option value="all">All Metrics</option>
          </select>
        </div>
      </div>
      
      <div className="relative h-80">
        <Line 
          ref={chartRef}
          data={chartData} 
          options={options} 
        />
      </div>

      {/* Summary stats */}
      <div className="mt-4 grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
        <div className="text-center">
          <div className="text-lg font-semibold text-indigo-600">
            {data[data.length - 1]?.newCustomers.toLocaleString() || '0'}
          </div>
          <div className="text-xs text-gray-500">Latest New</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-green-600">
            {data[data.length - 1]?.totalCustomers.toLocaleString() || '0'}
          </div>
          <div className="text-xs text-gray-500">Total Now</div>
        </div>
        <div className="text-center">
          <div className="text-lg font-semibold text-yellow-600">
            {data[data.length - 1]?.conversionRate.toFixed(1) || '0'}%
          </div>
          <div className="text-xs text-gray-500">Conversion Rate</div>
        </div>
      </div>
    </div>
  );
}