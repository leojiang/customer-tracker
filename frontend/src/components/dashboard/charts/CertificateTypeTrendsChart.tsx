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
import { CertificateTypeTranslationKeys } from '@/types/customer';
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
  selectedTypes?: string[];
  onSelectedTypesChange?: (types: string[]) => void;
}

// Color palette for different certificate types
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
];

// Function to get consistent color for certificate type based on hash
const getColorForCertificateType = (certificateType: string, index: number) => {
  return COLOR_PALETTE[index % COLOR_PALETTE.length];
};

// Function to convert enum name to translation key format
const convertEnumToTranslationKey = (enumName: string): string => {
  // Convert A_SPECIAL_EQUIPMENT_SAFETY to aSpecialEquipmentSafety
  return enumName.toLowerCase().replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
};

// Function to get localized certificate type name
const getLocalizedCertificateTypeName = (certificateType: string, t: (key: string) => string) => {
  const translationKey = `certificateType.${convertEnumToTranslationKey(certificateType)}`;
  const translated = t(translationKey);
  // If the translation key doesn't exist, return the original type name
  return translated !== translationKey ? translated : certificateType;
};

export default function CertificateTypeTrendsChart({
  data,
  title,
  className = "",
  loading = false,
  error = null,
  selectedTypes: controlledSelectedTypes,
  onSelectedTypesChange,
}: CertificateTypeTrendsChartProps) {
  const { t, language } = useLanguage();
  const chartRef = useRef<ChartJS<'line'>>(null);

  // Convert array to Set for internal use
  const [internalSelectedTypes, setInternalSelectedTypes] = useState<Set<string>>(new Set());

  // Determine which state to use
  const selectedTypesSet = controlledSelectedTypes !== undefined
    ? new Set(controlledSelectedTypes)
    : internalSelectedTypes;

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Initialize with all certificate types selected (only for uncontrolled mode)
  useEffect(() => {
    if (controlledSelectedTypes === undefined && data && data.trendsByCertificateType) {
      const allTypes = Object.keys(data.trendsByCertificateType).sort((a, b) => {
        const orderA = Object.keys(CertificateTypeTranslationKeys).indexOf(a);
        const orderB = Object.keys(CertificateTypeTranslationKeys).indexOf(b);
        return orderA - orderB;
      });
      setInternalSelectedTypes(new Set(allTypes));
    }
  }, [data, controlledSelectedTypes]);

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

  // Handle certificate type selection
  const handleTypeToggle = (type: string) => {
    const newSelected = new Set(selectedTypesSet);
    if (newSelected.has(type)) {
      newSelected.delete(type);
    } else {
      newSelected.add(type);
    }

    if (onSelectedTypesChange) {
      onSelectedTypesChange(Array.from(newSelected));
    } else {
      setInternalSelectedTypes(newSelected);
    }
  };

  // Handle toggle all certificate types
  const handleToggleAll = () => {
    if (selectedTypesSet.size === allTypes.length) {
      // If all are selected, deselect all
      const newSet = new Set<string>();
      if (onSelectedTypesChange) {
        onSelectedTypesChange(Array.from(newSet));
      } else {
        setInternalSelectedTypes(newSet);
      }
    } else {
      // Otherwise, select all
      const newSet = new Set(allTypes);
      if (onSelectedTypesChange) {
        onSelectedTypesChange(Array.from(newSet));
      } else {
        setInternalSelectedTypes(newSet);
      }
    }
  };

  // Get selected count text
  // Order certificate types the same as in CustomerForm (using CertificateTypeTranslationKeys order)
  const allTypes = Object.keys(data.trendsByCertificateType || {}).sort((a, b) => {
    const orderA = Object.keys(CertificateTypeTranslationKeys).indexOf(a);
    const orderB = Object.keys(CertificateTypeTranslationKeys).indexOf(b);
    return orderA - orderB;
  });
  const selectedCount = selectedTypesSet.size;
  const selectedText = selectedCount === allTypes.length
    ? t('dashboard.charts.allCertificateTypes')
    : `${selectedCount} ${t('dashboard.charts.certificateTypes')}`;

  // Filter certificate types based on selection and maintain order
  const filteredCertificateTypes = allTypes.filter(type => selectedTypesSet.has(type));

  // Use default title if not provided
  const chartTitle = title || t('dashboard.charts.certificateTypeTrends');

  // Get all unique dates across all certificate types
  const allDates = Array.from(
    new Set(
      Object.values(data.trendsByCertificateType).flat().map(point => point.date)
    )
  ).sort();

  // Prepare chart data with multiple datasets
  const certificateTypes = filteredCertificateTypes;

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
    datasets: certificateTypes.map((type) => {
      // Use the index from the full sorted list to ensure consistent colors
      const colorIndex = allTypes.indexOf(type);
      const colors = getColorForCertificateType(type, colorIndex);
      const typeData = data.trendsByCertificateType[type] || [];
      const localizedType = getLocalizedCertificateTypeName(type, t);

      return {
        label: localizedType,
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
                return `${label}: ${value.toLocaleString()} ${t('dashboard.charts.newCertifications')}`;
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

        {/* Certificate Type Selector Dropdown */}
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
                {/* All toggle */}
                <label className="flex items-center px-4 py-2 hover:bg-gray-100 cursor-pointer border-b border-gray-200">
                  <input
                    type="checkbox"
                    checked={selectedTypesSet.size === allTypes.length}
                    onChange={handleToggleAll}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                  />
                  <span className="ml-3 text-sm font-medium text-gray-700">
                    {t('dashboard.charts.all')}
                  </span>
                </label>
                {allTypes.map((type) => {
                  const colors = getColorForCertificateType(type, allTypes.indexOf(type));
                  const localizedType = getLocalizedCertificateTypeName(type, t);
                  return (
                    <label
                      key={type}
                      className="flex items-center px-4 py-2 hover:bg-gray-100 cursor-pointer"
                    >
                      <input
                        type="checkbox"
                        checked={selectedTypesSet.has(type)}
                        onChange={() => handleTypeToggle(type)}
                        className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                      />
                      <span
                        className="ml-3 text-sm text-gray-700"
                        style={{ color: selectedTypesSet.has(type) ? colors!.border : undefined }}
                      >
                        {localizedType}
                      </span>
                      <div
                        className="ml-auto w-3 h-3 rounded-full"
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