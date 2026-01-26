'use client';

import { Copy, Check, X, Key } from 'lucide-react';
import { useState } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';

interface PasswordResetSuccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  temporaryPassword: string;
  userPhone: string;
}

export default function PasswordResetSuccessModal({
  isOpen,
  onClose,
  temporaryPassword,
  userPhone,
}: PasswordResetSuccessModalProps) {
  const { t } = useLanguage();
  const [copied, setCopied] = useState(false);

  if (!isOpen) {
    return null;
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(temporaryPassword);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="bg-blue-100 rounded-full p-2">
              <Key className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900">
              {t('userManagement.passwordResetSuccess')}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <p className="text-gray-600 mb-4">
            {t('userManagement.passwordResetSuccessMessage')} {userPhone}
          </p>

          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('userManagement.temporaryPassword')}
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={temporaryPassword}
                className="flex-1 px-3 py-2 bg-white border border-gray-300 rounded-md font-mono text-lg font-semibold text-gray-900"
              />
              <button
                onClick={handleCopy}
                className="px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center gap-2"
                title={t('userManagement.copyPassword')}
              >
                {copied ? (
                  <>
                    <Check size={18} />
                    <span className="text-sm">{t('userManagement.copied')}</span>
                  </>
                ) : (
                  <>
                    <Copy size={18} />
                    <span className="text-sm">{t('userManagement.copy')}</span>
                  </>
                )}
              </button>
            </div>
          </div>

          <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p className="text-sm text-yellow-800">
              <strong>{t('userManagement.important')}:</strong>{' '}
              {t('userManagement.passwordResetWarning')}
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end px-6 py-4 bg-gray-50 rounded-b-lg">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
          >
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  );
}
