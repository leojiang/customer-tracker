'use client';

import { formatDistanceToNow } from 'date-fns';
import { 
  Phone, 
  Mail, 
  User, 
  CheckCircle, 
  Clock, 
  AlertCircle,
  TrendingUp,
  ArrowRight
} from 'lucide-react';

interface ActivityItem {
  id: string;
  type: 'call' | 'email' | 'status_change' | 'customer_added' | 'conversion' | 'meeting';
  title: string;
  description?: string;
  timestamp: string;
  customerName?: string;
  salesPerson?: string;
  metadata?: {
    fromStatus?: string;
    toStatus?: string;
    phone?: string;
    email?: string;
  };
}

interface ActivityFeedProps {
  title?: string;
  activities: ActivityItem[];
  maxItems?: number;
  className?: string;
  loading?: boolean;
  showSalesPerson?: boolean;
}

export default function ActivityFeed({
  title = "Recent Activities",
  activities,
  maxItems = 10,
  className = '',
  loading = false,
  showSalesPerson = false
}: ActivityFeedProps) {
  const getActivityIcon = (type: ActivityItem['type']) => {
    const iconProps = { size: 16, className: "flex-shrink-0" };
    
    switch (type) {
      case 'call':
        return <Phone {...iconProps} className="flex-shrink-0 text-blue-600" />;
      case 'email':
        return <Mail {...iconProps} className="flex-shrink-0 text-green-600" />;
      case 'status_change':
        return <ArrowRight {...iconProps} className="flex-shrink-0 text-yellow-600" />;
      case 'customer_added':
        return <User {...iconProps} className="flex-shrink-0 text-indigo-600" />;
      case 'conversion':
        return <TrendingUp {...iconProps} className="flex-shrink-0 text-green-600" />;
      case 'meeting':
        return <Clock {...iconProps} className="flex-shrink-0 text-purple-600" />;
      default:
        return <CheckCircle {...iconProps} className="flex-shrink-0 text-gray-600" />;
    }
  };

  const formatStatusName = (status: string): string => {
    return status
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  };

  const displayedActivities = activities.slice(0, maxItems);

  if (loading) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <div className="animate-pulse">
          <div className="h-5 bg-gray-200 rounded w-32 mb-4"></div>
          {[1, 2, 3, 4, 5].map(i => (
            <div key={i} className="flex items-start space-x-3 mb-4 last:mb-0">
              <div className="w-4 h-4 bg-gray-200 rounded-full mt-1"></div>
              <div className="flex-1">
                <div className="h-4 bg-gray-200 rounded w-48 mb-1"></div>
                <div className="h-3 bg-gray-200 rounded w-32"></div>
              </div>
              <div className="h-3 bg-gray-200 rounded w-16"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (!activities || activities.length === 0) {
    return (
      <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
        <h3 className="text-lg font-medium text-gray-900 mb-4">{title}</h3>
        <div className="text-center py-8">
          <AlertCircle size={24} className="mx-auto text-gray-400 mb-2" />
          <p className="text-gray-500 text-sm">No recent activities</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-medium text-gray-900">{title}</h3>
        <span className="text-sm text-gray-500">
          {activities.length} {activities.length === 1 ? 'activity' : 'activities'}
        </span>
      </div>

      <div className="flow-root">
        <ul className="-mb-8">
          {displayedActivities.map((activity, index) => (
            <li key={activity.id}>
              <div className="relative pb-8">
                {index !== displayedActivities.length - 1 && (
                  <span
                    className="absolute left-2 top-6 -ml-px h-full w-0.5 bg-gray-200"
                    aria-hidden="true"
                  />
                )}
                
                <div className="relative flex items-start space-x-3">
                  <div className="relative">
                    <div className="flex h-4 w-4 items-center justify-center bg-white border-2 border-gray-300 rounded-full">
                      {getActivityIcon(activity.type)}
                    </div>
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="text-sm">
                      <div className="font-medium text-gray-900">
                        {activity.title}
                        {activity.customerName && (
                          <span className="ml-1 font-normal text-gray-600">
                            • {activity.customerName}
                          </span>
                        )}
                      </div>
                      
                      {activity.description && (
                        <p className="mt-1 text-gray-500 text-xs">
                          {activity.description}
                        </p>
                      )}
                      
                      {activity.metadata?.fromStatus && activity.metadata?.toStatus && (
                        <div className="mt-1 text-xs text-gray-500">
                          <span className="inline-flex items-center">
                            <span className="px-2 py-1 bg-gray-100 rounded text-xs">
                              {formatStatusName(activity.metadata.fromStatus)}
                            </span>
                            <ArrowRight size={12} className="mx-1" />
                            <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">
                              {formatStatusName(activity.metadata.toStatus)}
                            </span>
                          </span>
                        </div>
                      )}
                      
                      {showSalesPerson && activity.salesPerson && (
                        <p className="mt-1 text-xs text-gray-400">
                          by {activity.salesPerson}
                        </p>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex-shrink-0 text-right">
                    <time className="text-xs text-gray-400" dateTime={activity.timestamp}>
                      {formatDistanceToNow(new Date(activity.timestamp), { addSuffix: true })}
                    </time>
                  </div>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
      
      {activities.length > maxItems && (
        <div className="mt-4 pt-4 border-t border-gray-200">
          <button className="text-sm text-indigo-600 hover:text-indigo-500 font-medium">
            View all {activities.length} activities
          </button>
        </div>
      )}
    </div>
  );
}