'use client';

import { useState } from 'react';
import { X, CheckCircle, XCircle, Clock } from 'lucide-react';

interface ApprovalModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (reason?: string) => void;
  type: 'approve' | 'reject' | 'reset';
  userPhone: string;
  loading?: boolean;
}

export default function ApprovalModal({
  isOpen,
  onClose,
  onConfirm,
  type,
  userPhone,
  loading = false
}: ApprovalModalProps) {
  const [reason, setReason] = useState('');

  if (!isOpen) {
    return null;
  }

  const getModalConfig = () => {
    switch (type) {
      case 'approve':
        return {
          title: 'Approve User Registration',
          description: `Approve registration for ${userPhone}?`,
          icon: <CheckCircle className="w-6 h-6 text-green-600" />,
          buttonText: 'Approve User',
          buttonClass: 'bg-green-600 hover:bg-green-700 text-white',
          reasonLabel: 'Approval reason (optional)',
          reasonPlaceholder: 'e.g., Application looks good, verified identity...'
        };
      case 'reject':
        return {
          title: 'Reject User Registration',
          description: `Reject registration for ${userPhone}?`,
          icon: <XCircle className="w-6 h-6 text-red-600" />,
          buttonText: 'Reject User',
          buttonClass: 'bg-red-600 hover:bg-red-700 text-white',
          reasonLabel: 'Rejection reason *',
          reasonPlaceholder: 'e.g., Incomplete application, verification failed...'
        };
      case 'reset':
        return {
          title: 'Reset User Status',
          description: `Reset ${userPhone} status back to pending?`,
          icon: <Clock className="w-6 h-6 text-orange-600" />,
          buttonText: 'Reset Status',
          buttonClass: 'bg-orange-600 hover:bg-orange-700 text-white',
          reasonLabel: 'Reset reason (optional)',
          reasonPlaceholder: 'e.g., Need to review application again...'
        };
      default:
        return {
          title: '',
          description: '',
          icon: null,
          buttonText: '',
          buttonClass: '',
          reasonLabel: '',
          reasonPlaceholder: ''
        };
    }
  };

  const config = getModalConfig();

  const handleConfirm = () => {
    if (type === 'reject' && !reason.trim()) {
      return; // Rejection requires a reason
    }
    onConfirm(reason.trim() || undefined);
  };

  const handleClose = () => {
    setReason('');
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            {config.icon}
            <h3 className="text-lg font-semibold text-gray-900">{config.title}</h3>
          </div>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={loading}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <p className="text-gray-600 mb-4">{config.description}</p>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                {config.reasonLabel}
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none"
                placeholder={config.reasonPlaceholder}
                disabled={loading}
              />
              {type === 'reject' && (
                <p className="text-sm text-red-600 mt-1">* Rejection reason is required</p>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 bg-gray-50 rounded-b-lg">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition-colors"
            disabled={loading}
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading || (type === 'reject' && !reason.trim())}
            className={`px-4 py-2 text-sm font-medium rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${config.buttonClass}`}
          >
            {loading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                Processing...
              </div>
            ) : (
              config.buttonText
            )}
          </button>
        </div>
      </div>
    </div>
  );
}