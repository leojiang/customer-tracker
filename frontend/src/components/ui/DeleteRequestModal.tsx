'use client';

import { useState, useEffect } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { AlertTriangle, X, CheckCircle } from 'lucide-react';

interface DeleteRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (reason: string) => Promise<void>;
  customerName: string;
}

export default function DeleteRequestModal({
  isOpen,
  onClose,
  onSubmit,
  customerName,
}: DeleteRequestModalProps) {
  const { t } = useLanguage();
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setReason('');
      setError(null);
      setShowSuccess(false);
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!reason.trim()) {
      setError(t('deleteRequests.reasonRequired'));
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      await onSubmit(reason);
      setReason('');
      setShowSuccess(true);
      // Auto-close after success message
      setTimeout(() => {
        onClose();
      }, 2000);
    } catch (err) {
      // Check if it's the specific error about pending request
      const errorMessage = err instanceof Error ? err.message : String(err);
      if (errorMessage.includes('pending delete request already exists')) {
        setError(t('deleteRequests.pendingRequestExists'));
      } else {
        setError(errorMessage || t('deleteRequests.requestDeleteError'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) {
    return null;
  }

  // Show success state
  if (showSuccess) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-8 text-center">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            {t('deleteRequests.requestSubmitted')}
          </h2>
          <p className="text-sm text-gray-600">
            {t('deleteRequests.requestDeleteSuccess')}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-red-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900">
              {t('deleteRequests.requestDeletion')}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={isSubmitting}
          >
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6">
          <div className="mb-4">
            <p className="text-sm text-gray-600 mb-4">
              {t('customers.detail.customer')}: <span className="font-semibold">{customerName}</span>
            </p>

            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('deleteRequests.reason')} <span className="text-red-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={t('deleteRequests.reasonPlaceholder')}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
              disabled={isSubmitting}
              maxLength={1000}
            />
            <p className="text-xs text-gray-500 mt-1">
              {reason.length}/1000
            </p>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {t('customers.cancel')}
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !reason.trim()}
              className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? t('deleteRequests.submitting') : t('deleteRequests.requestDeletion')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
