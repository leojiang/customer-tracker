'use client';

import { useState, useEffect } from 'react';
import { Clock, ArrowRight, MessageSquare } from 'lucide-react';
import { StatusHistory as StatusHistoryType } from '@/types/customer';
import { customerApi } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import { format } from 'date-fns';

interface StatusHistoryProps {
  customerId: string;
  refreshTrigger?: number; // Optional prop to trigger refresh
}

export default function StatusHistory({ customerId, refreshTrigger }: StatusHistoryProps) {
  const [history, setHistory] = useState<StatusHistoryType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadHistory = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await customerApi.getStatusHistory(customerId);
        setHistory(data.sort((a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime()));
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load status history');
      } finally {
        setLoading(false);
      }
    };

    loadHistory();
  }, [customerId, refreshTrigger]);

  if (loading) {
    return (
      <div className="card-elevated flex flex-col h-full">
        <div className="card-header flex-shrink-0">
          <h3 className="text-headline-6 flex items-center gap-3">
            <Clock size={20} className="text-surface-500" />
            Status History
          </h3>
        </div>
        <div className="card-content flex-grow flex items-center justify-center">
          <div className="text-center">
            <div className="loading-skeleton w-16 h-16 rounded-full mx-auto mb-4"></div>
            <div className="text-body-1 text-surface-600">Loading history...</div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card-elevated flex flex-col h-full">
        <div className="card-header flex-shrink-0">
          <h3 className="text-headline-6 flex items-center gap-3">
            <Clock size={20} className="text-surface-500" />
            Status History
          </h3>
        </div>
        <div className="card-content flex-grow flex items-center justify-center">
          <div className="text-center">
            <div className="text-red-600 mb-2">Error loading history</div>
            <p className="text-body-2 text-surface-600">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (history.length === 0) {
    return (
      <div className="card-elevated flex flex-col h-full">
        <div className="card-header flex-shrink-0">
          <h3 className="text-headline-6 flex items-center gap-3">
            <Clock size={20} className="text-surface-500" />
            Status History
          </h3>
        </div>
        <div className="card-content flex-grow flex items-center justify-center">
          <div className="text-center">
            <div className="w-16 h-16 bg-surface-200 rounded-full mx-auto mb-4 flex items-center justify-center">
              <Clock size={24} className="text-surface-400" />
            </div>
            <div className="text-body-1 text-surface-600">No status history available</div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card-elevated flex flex-col h-full">
      <div className="card-header flex-shrink-0">
        <h3 className="text-headline-6 flex items-center gap-3">
          <Clock size={20} className="text-surface-500" />
          Status History
        </h3>
      </div>
      <div className="card-content flex-grow overflow-hidden">
        <div className="space-y-6 overflow-y-auto max-h-80 pr-2" style={{
          scrollbarWidth: 'thin',
          scrollbarColor: '#cbd5e1 #f1f5f9'
        }}>
          {history.map((item, index) => (
            <div key={item.id} className="relative">
              {index < history.length - 1 && (
                <div className="absolute left-4 top-10 bottom-0 w-px bg-surface-200" />
              )}
              
              <div className="flex items-start gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center mt-1 shadow-md-1">
                  <div className="w-3 h-3 bg-primary-600 rounded-full" />
                </div>
                
                <div className="flex-1 min-w-0 pt-1">
                  <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-3">
                    {item.fromStatus ? (
                      <div className="flex items-center gap-3">
                        <StatusBadge status={item.fromStatus} />
                        <ArrowRight size={16} className="text-surface-400" />
                        <StatusBadge status={item.toStatus} />
                      </div>
                    ) : (
                      <div className="flex items-center gap-3">
                        <span className="text-body-2 text-surface-600">Initial status:</span>
                        <StatusBadge status={item.toStatus} />
                      </div>
                    )}
                  </div>
                  
                  <div className="text-caption text-surface-500 mb-3">
                    {format(new Date(item.changedAt), 'MMM dd, yyyy HH:mm')}
                  </div>
                  
                  {item.reason && (
                    <div className="bg-surface-50 border border-surface-200 rounded-lg p-3 mt-3">
                      <div className="flex items-start gap-2">
                        <MessageSquare size={16} className="text-surface-400 mt-0.5 flex-shrink-0" />
                        <span className="text-body-2 text-surface-700">{item.reason}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}