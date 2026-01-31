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

interface AgentPerformanceTrendsData {
  trendsByAgent: Record<string, TrendDataPoint[]>;
  agents: string[];
}

interface AgentPerformanceTrendsChartProps {
  data: AgentPerformanceTrendsData;
  title?: string;
  className?: string;
  loading?: boolean;
  error?: string | null;
  selectedAgents?: string[];
  onSelectedAgentsChange?: (agents: string[]) => void;
}

// Color palette for different agents (more colors for many agents)
const COLOR_PALETTE = [
  { border: 'rgb(99, 102, 241)', background: 'rgba(99, 102, 241, 0.1)' },     // indigo-500
  { border: 'rgb(34, 197, 94)', background: 'rgba(34, 197, 94, 0.1)' },       // green-500
  { border: 'rgb(234, 179, 8)', background: 'rgba(234, 179, 8, 0.1)' },       // yellow-500
  { border: 'rgb(239, 68, 68)', background: 'rgba(239, 68, 68, 0.1)' },       // red-500
  { border: 'rgb(168, 85, 247)', background: 'rgba(168, 85, 247, 0.1)' },     // purple-500
  { border: 'rgb(6, 182, 212)', background: 'rgba(6, 182, 212, 0.1)' },       // cyan-500
  { border: 'rgb(249, 115, 22)', background: 'rgba(249, 115, 22, 0.1)' },      // orange-500
  { border: 'rgb(236, 72, 153)', background: 'rgba(236, 72, 153, 0.1)' },     // pink-500
  { border: 'rgb(20, 184, 166)', background: 'rgba(20, 184, 166, 0.1)' },     // teal-500
  { border: 'rgb(139, 92, 246)', background: 'rgba(139, 92, 246, 0.1)' },     // violet-500
  { border: 'rgb(37, 99, 235)', background: 'rgba(37, 99, 235, 0.1)' },       // blue-600
  { border: 'rgb(22, 163, 74)', background: 'rgba(22, 163, 74, 0.1)' },        // green-600
  { border: 'rgb(202, 138, 4)', background: 'rgba(202, 138, 4, 0.1)' },        // yellow-600
  { border: 'rgb(220, 38, 38)', background: 'rgba(220, 38, 38, 0.1)' },        // red-600
  { border: 'rgb(147, 51, 234)', background: 'rgba(147, 51, 234, 0.1)' },      // purple-600
];

// Function to get consistent color for agent based on hash
const getColorForAgent = (agent: string, index: number) => {
  return COLOR_PALETTE[index % COLOR_PALETTE.length];
};

export default function AgentPerformanceTrendsChart({
  data,
  title,
  className = "",
  loading = false,
  error = null,
  selectedAgents: controlledSelectedAgents,
  onSelectedAgentsChange,
}: AgentPerformanceTrendsChartProps) {
  const { t, language } = useLanguage();
  const chartRef = useRef<ChartJS<'line'>>(null);

  // Convert array to Set for internal use
  const [internalSelectedAgents, setInternalSelectedAgents] = useState<Set<string>>(new Set());

  // Determine which state to use
  const selectedAgentsSet = controlledSelectedAgents !== undefined
    ? new Set(controlledSelectedAgents)
    : internalSelectedAgents;

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Initialize with all agents selected (only for uncontrolled mode)
  useEffect(() => {
    if (controlledSelectedAgents === undefined && data && data.agents && data.agents.length > 0) {
      // Select all agents by default
      setInternalSelectedAgents(new Set(data.agents));
    }
  }, [data, controlledSelectedAgents]);

  // Handle click outside to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Handle agent selection
  const handleAgentToggle = (agent: string) => {
    const newSelected = new Set(selectedAgentsSet);
    if (newSelected.has(agent)) {
      newSelected.delete(agent);
    } else {
      newSelected.add(agent);
    }

    if (onSelectedAgentsChange) {
      onSelectedAgentsChange(Array.from(newSelected));
    } else {
      setInternalSelectedAgents(newSelected);
    }
  };

  // Handle toggle all agents
  const handleToggleAll = () => {
    if (selectedAgentsSet.size === allAgents.length) {
      // If all are selected, deselect all
      const newSet = new Set<string>();
      if (onSelectedAgentsChange) {
        onSelectedAgentsChange(Array.from(newSet));
      } else {
        setInternalSelectedAgents(newSet);
      }
    } else {
      // Otherwise, select all
      const newSet = new Set(allAgents);
      if (onSelectedAgentsChange) {
        onSelectedAgentsChange(Array.from(newSet));
      } else {
        setInternalSelectedAgents(newSet);
      }
    }
  };

  // Get all agents from the data
  const allAgents = data.agents || [];
  const selectedCount = selectedAgentsSet.size;
  const selectedText = selectedCount === allAgents.length
    ? t('dashboard.charts.allAgents')
    : `${selectedCount} ${t('dashboard.charts.agents')}`;

  // Filter agents based on selection
  const filteredAgents = allAgents.filter(agent => selectedAgentsSet.has(agent));

  // Use default title if not provided
  const chartTitle = title || t('dashboard.charts.agentPerformanceTrends');

  // Get all unique dates across all agents
  const allDates = Array.from(
    new Set(
      Object.values(data.trendsByAgent || {}).flat().map(point => point.date)
    )
  ).sort();

  // Prepare chart data with multiple datasets
  const chartData = {
    labels: allDates.map(dateStr => {
      // Handle both YYYY-MM and YYYY-MM-DD formats
      if (/^\d{4}-\d{2}$/.test(dateStr)) {
        // Monthly format: use first day of the month
        const [year, month] = dateStr.split('-');
        return new Date(parseInt(year || '2024'), parseInt(month || '01') - 1, 1);
      } else {
        // Daily format
        return new Date(dateStr);
      }
    }),
    datasets: filteredAgents.map((agent) => {
      // Use the index from the full sorted list to ensure consistent colors
      const colorIndex = allAgents.indexOf(agent);
      const colors = getColorForAgent(agent, colorIndex);
      const agentData = data.trendsByAgent[agent] || [];

      return {
        label: agent,
        data: allDates.map(date => {
          const dataPoint = agentData.find(point => point.date === date);
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
        hidden: false,
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
        display: false, // Hide legend since we have the dropdown selector
      },
      title: {
        display: false, // Hide title since we have it above the chart
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
                const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';
                return date.toLocaleDateString(localeCode, {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric'
                });
              }
            }
            return '';
          },
          beforeBody: function(tooltipItems) {
            // Sort items by value in descending order
            const sortedItems = tooltipItems
              .filter(item => item.parsed.y !== null && item.parsed.y > 0)
              .sort((a, b) => (b.parsed.y || 0) - (a.parsed.y || 0));

            // Build the custom body
            return sortedItems.map(item => {
              const label = item.dataset.label || '';
              const value = item.parsed.y;
              if (typeof value === 'number') {
                return `${label}: ${value.toLocaleString()} ${t('dashboard.charts.newCustomers')}`;
              }
              return `${label}: N/A`;
            }).join('\n');
          },
          label: function() {
            // Return empty string to hide default labels since we're using beforeBody
            return '';
          },
        },
        // Custom filter to only show items with values > 0
        filter: function(item) {
          return item.parsed.y !== null && item.parsed.y > 0;
        },
      },
    },
    scales: {
      x: {
        type: 'time' as const,
        time: {
          unit: 'month',
          displayFormats: {
            month: 'MMM yyyy',
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
          callback: function(value) {
            if (typeof value === 'number') {
              const date = new Date(value);
              const localeCode = language === 'zh-CN' ? 'zh-CN' : 'en-US';
              return date.toLocaleDateString(localeCode, {
                year: 'numeric',
                month: 'short'
              });
            }
            return value;
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
          text: t('dashboard.charts.newCustomers'),
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

  if (!data || !data.trendsByAgent || Object.keys(data.trendsByAgent).length === 0) {
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

        {/* Agent Selector Dropdown */}
        <div className="relative" ref={dropdownRef}>
          <button
            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            className="px-4 py-2 bg-white border border-gray-300 rounded-lg shadow-sm text-left flex items-center justify-between hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 min-w-[200px]"
          >
            <span className="text-sm text-gray-700">{selectedText}</span>
            <svg
              className={`w-5 h-5 text-gray-400 transition-transform duration-200 ${isDropdownOpen ? 'transform rotate-180' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          {isDropdownOpen && (
            <div className="absolute z-10 right-0 mt-2 w-64 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-auto">
              <div className="py-1">
                {/* Toggle All */}
                <label className="flex items-center px-4 py-2 hover:bg-gray-100 cursor-pointer border-b border-gray-200">
                  <input
                    type="checkbox"
                    checked={selectedAgentsSet.size === allAgents.length}
                    onChange={handleToggleAll}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                  />
                  <span className="ml-3 text-sm font-medium text-gray-700">
                    {t('dashboard.charts.all')}
                  </span>
                </label>
                {allAgents.map((agent) => {
                  const colors = getColorForAgent(agent, allAgents.indexOf(agent));
                  return (
                    <label
                      key={agent}
                      className="flex items-center px-4 py-2 hover:bg-gray-100 cursor-pointer"
                    >
                      <input
                        type="checkbox"
                        checked={selectedAgentsSet.has(agent)}
                        onChange={() => handleAgentToggle(agent)}
                        className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                      />
                      <span
                        className="ml-3 text-sm text-gray-700 truncate"
                        style={{ color: selectedAgentsSet.has(agent) ? colors!.border : undefined }}
                        title={agent}
                      >
                        {agent}
                      </span>
                      <div
                        className="ml-auto w-3 h-3 rounded-full flex-shrink-0"
                        style={{ backgroundColor: colors!.border }}
                      />
                    </label>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      <div style={{ height: '350px' }}>
        <Line ref={chartRef} data={chartData} options={chartOptions} />
      </div>
    </div>
  );
}
