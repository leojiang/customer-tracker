'use client';

import { CheckCircle, Circle, Clock, Target } from 'lucide-react';

interface ProgressStep {
  id: string;
  label: string;
  value: number;
  total: number;
  status: 'completed' | 'in-progress' | 'pending';
  description?: string;
}

interface ProgressWidgetProps {
  title: string;
  steps: ProgressStep[];
  className?: string;
  loading?: boolean;
  showPercentages?: boolean;
}

export default function ProgressWidget({
  title,
  steps,
  className = '',
  loading = false,
  showPercentages = true
}: ProgressWidgetProps) {
  const getStatusIcon = (status: ProgressStep['status']) => {
    switch (status) {
      case 'completed':
        return <CheckCircle size={20} className="text-green-600" />;
      case 'in-progress':
        return <Clock size={20} className="text-yellow-600" />;
      default:
        return <Circle size={20} className="text-gray-400" />;
    }
  };

  const getProgressBarColor = (status: ProgressStep['status']) => {
    switch (status) {
      case 'completed':
        return 'bg-green-600';
      case 'in-progress':
        return 'bg-yellow-600';
      default:
        return 'bg-gray-300';
    }
  };

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="h-5 bg-gray-200 rounded w-48 mb-6"></div>
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="flex items-center justify-between mb-4 last:mb-0">
              <div className="flex items-center">
                <div className="w-5 h-5 bg-gray-200 rounded-full mr-3"></div>
                <div className="h-4 bg-gray-200 rounded w-32"></div>
              </div>
              <div className="flex items-center">
                <div className="w-16 bg-gray-200 rounded-full h-2 mr-2"></div>
                <div className="h-4 bg-gray-200 rounded w-8"></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <Target size={20} className="text-gray-400" />
      </div>

      <div className="space-y-4">
        {steps.map((step) => {
          const percentage = step.total > 0 ? (step.value / step.total) * 100 : 0;
          
          return (
            <div key={step.id} className="flex items-center justify-between">
              <div className="flex items-center flex-1 min-w-0">
                <div className="flex-shrink-0 mr-3">
                  {getStatusIcon(step.status)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {step.label}
                  </p>
                  {step.description && (
                    <p className="text-xs text-gray-500 truncate">
                      {step.description}
                    </p>
                  )}
                </div>
              </div>
              
              <div className="flex items-center ml-4">
                <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-300 ${getProgressBarColor(step.status)}`}
                    style={{ width: `${Math.min(percentage, 100)}%` }}
                  />
                </div>
                <div className="text-right min-w-0 w-12">
                  {showPercentages ? (
                    <span className="text-xs font-medium text-gray-900">
                      {percentage.toFixed(0)}%
                    </span>
                  ) : (
                    <span className="text-xs font-medium text-gray-900">
                      {step.value}
                    </span>
                  )}
                  <div className="text-xs text-gray-500">
                    /{step.total}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      
      {/* Overall Progress Summary */}
      <div className="mt-6 pt-4 border-t border-gray-200">
        <div className="flex justify-between text-sm">
          <span className="font-medium text-gray-700">Overall Progress</span>
          <span className="text-gray-500">
            {steps.reduce((acc, step) => acc + step.value, 0)} / {steps.reduce((acc, step) => acc + step.total, 0)}
          </span>
        </div>
        <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
          <div
            className="bg-indigo-600 h-2 rounded-full transition-all duration-500"
            style={{
              width: `${Math.min(
                (steps.reduce((acc, step) => acc + step.value, 0) / 
                 Math.max(steps.reduce((acc, step) => acc + step.total, 0), 1)) * 100,
                100
              )}%`
            }}
          />
        </div>
      </div>
    </div>
  );
}