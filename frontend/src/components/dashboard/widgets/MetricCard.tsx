'use client';

import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  change?: number;
  trend?: 'up' | 'down' | 'flat';
  description?: string;
  icon?: React.ReactNode;
  className?: string;
  loading?: boolean;
}

export default function MetricCard({
  title,
  value,
  change,
  trend,
  description,
  icon,
  className = '',
  loading = false
}: MetricCardProps) {
  const formatValue = (val: string | number): string => {
    if (typeof val === 'number') {
      return val.toLocaleString();
    }
    return val;
  };

  const getTrendColor = (trendType?: 'up' | 'down' | 'flat'): string => {
    switch (trendType) {
      case 'up':
        return 'text-green-600';
      case 'down':
        return 'text-red-600';
      case 'flat':
        return 'text-gray-500';
      default:
        return change !== undefined && change >= 0 ? 'text-green-600' : 'text-red-600';
    }
  };

  const getTrendIcon = (trendType?: 'up' | 'down' | 'flat') => {
    switch (trendType) {
      case 'up':
        return <TrendingUp size={16} />;
      case 'down':
        return <TrendingDown size={16} />;
      case 'flat':
        return <Minus size={16} />;
      default:
        return change !== undefined && change >= 0 ? <TrendingUp size={16} /> : <TrendingDown size={16} />;
    }
  };

  if (loading) {
    return (
      <div className={`overflow-hidden rounded-lg bg-white px-4 py-5 shadow sm:p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="flex items-center justify-between">
            <div className="h-4 bg-gray-200 rounded w-32"></div>
            {icon && <div className="h-8 w-8 bg-gray-200 rounded"></div>}
          </div>
          <div className="mt-2">
            <div className="h-8 bg-gray-200 rounded w-24"></div>
          </div>
          <div className="mt-2">
            <div className="h-4 bg-gray-200 rounded w-28"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`overflow-hidden rounded-lg bg-white px-4 py-5 shadow hover:shadow-md transition-shadow sm:p-6 ${className}`}>
      <div className="flex items-center justify-between">
        <dt className="truncate text-sm font-medium text-gray-500">
          {title}
        </dt>
        {icon && (
          <div className="flex-shrink-0">
            <div className="h-8 w-8 text-gray-400">
              {icon}
            </div>
          </div>
        )}
      </div>
      
      <dd className="mt-1 text-3xl font-semibold tracking-tight text-gray-900">
        {formatValue(value)}
      </dd>
      
      {(change !== undefined || description) && (
        <div className="mt-2 flex items-center text-sm">
          {change !== undefined && (
            <div className={`flex items-center font-medium ${getTrendColor(trend)}`}>
              {getTrendIcon(trend)}
              <span className="ml-1">
                {change >= 0 ? '+' : ''}{change.toFixed(1)}%
              </span>
            </div>
          )}
          {description && (
            <span className={`ml-1 text-gray-500 ${change !== undefined ? 'ml-2' : ''}`}>
              {description}
            </span>
          )}
        </div>
      )}
    </div>
  );
}