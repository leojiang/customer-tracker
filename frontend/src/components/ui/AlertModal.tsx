'use client';

import { useLanguage } from '@/contexts/LanguageContext';
import { AlertCircle, X } from 'lucide-react';

interface AlertModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  message: string;
  type?: 'error' | 'warning' | 'info';
  confirmMode?: boolean;
  onConfirm?: () => void;
  confirmText?: string;
  cancelText?: string;
}

export default function AlertModal({
  isOpen,
  onClose,
  title,
  message,
  type = 'error',
  confirmMode = false,
  onConfirm,
  confirmText,
  cancelText,
}: AlertModalProps) {
  const { t } = useLanguage();

  if (!isOpen) {
    return null;
  }

  const getTypeStyles = () => {
    switch (type) {
      case 'warning':
        return {
          iconBg: 'bg-yellow-100',
          iconColor: 'text-yellow-600',
          borderColor: 'border-yellow-200',
        };
      case 'info':
        return {
          iconBg: 'bg-blue-100',
          iconColor: 'text-blue-600',
          borderColor: 'border-blue-200',
        };
      case 'error':
      default:
        return {
          iconBg: 'bg-red-100',
          iconColor: 'text-red-600',
          borderColor: 'border-red-200',
        };
    }
  };

  const styles = getTypeStyles();

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
        {/* Header */}
        <div className={`flex items-center justify-between p-6 border-b ${styles.borderColor}`}>
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 ${styles.iconBg} rounded-full flex items-center justify-center`}>
              <AlertCircle className={`w-5 h-5 ${styles.iconColor}`} />
            </div>
            <h3 className="text-lg font-medium text-gray-900">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <span className="sr-only">{t('customers.close')}</span>
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <div className="p-6">
          <p className="text-sm text-gray-700">
            {message}
          </p>
        </div>

        {/* Footer */}
        <div className={`flex justify-end gap-3 p-6 border-t ${styles.borderColor}`}>
          {confirmMode ? (
            <>
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                {cancelText || t('customers.cancel')}
              </button>
              <button
                type="button"
                onClick={() => {
                  onConfirm?.();
                  onClose();
                }}
                className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 border border-transparent rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                {confirmText || t('customers.confirm')}
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              {t('customers.ok')}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
