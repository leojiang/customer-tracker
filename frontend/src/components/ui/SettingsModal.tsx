'use client';

import { useEffect, useState } from 'react';
import { X, Settings, Globe, LogOut, Key } from 'lucide-react';
import { useLanguage, Language } from '@/contexts/LanguageContext';
import ChangePasswordForm from './ChangePasswordForm';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLogout: () => void;
}

export default function SettingsModal({ isOpen, onClose, onLogout }: SettingsModalProps) {
  const { language, setLanguage, t } = useLanguage();
  const [showChangePassword, setShowChangePassword] = useState(false);

  const handleLanguageChange = (newLanguage: Language) => {
    setLanguage(newLanguage);
  };

  const handleChangePassword = () => {
    setShowChangePassword(true);
  };

  const handleChangePasswordSuccess = () => {
    setShowChangePassword(false);
    onClose();
  };

  // Block body scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    // Cleanup on unmount
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg w-full max-w-md h-full max-h-screen flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200 flex-shrink-0">
          <div className="flex items-center gap-2">
            <Settings size={20} className="text-primary-600" />
            <h2 className="text-lg font-semibold text-gray-900">{t('settings.title')}</h2>
          </div>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X size={24} />
          </button>
        </div>

        {/* Settings Content */}
        <div className="flex-1 p-4 overflow-y-auto">
          {/* Language Setting */}
          <div className="space-y-4">
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Globe size={18} className="text-gray-600" />
                <label className="text-sm font-medium text-gray-900">{t('settings.language')}</label>
              </div>

              <div className="space-y-2">
                <label className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                  <input
                    type="radio"
                    name="language"
                    value="en"
                    checked={language === 'en'}
                    onChange={(e) => handleLanguageChange(e.target.value as Language)}
                    className="text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-900">{t('settings.language.en')}</span>
                </label>

                <label className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                  <input
                    type="radio"
                    name="language"
                    value="zh-CN"
                    checked={language === 'zh-CN'}
                    onChange={(e) => handleLanguageChange(e.target.value as Language)}
                    className="text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-900">{t('settings.language.zh-CN')}</span>
                </label>
              </div>
            </div>

            {/* Change Password Setting */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Key size={18} className="text-gray-600" />
                <label className="text-sm font-medium text-gray-900">{t('password.title')}</label>
              </div>

              <button
                onClick={handleChangePassword}
                className="w-full flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <Key size={16} className="text-gray-600" />
                <span className="text-sm text-gray-900">{t('password.button.change')}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 flex-shrink-0 mt-auto">
          <button 
            onClick={onLogout}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors"
          >
            <LogOut size={16} />
            {t('auth.logout')}
          </button>
        </div>
      </div>

      {/* Change Password Modal */}
      {showChangePassword && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
          onClick={() => setShowChangePassword(false)}
        >
          <div
            className="bg-white rounded-lg w-full max-w-md"
            onClick={(e) => e.stopPropagation()}
          >
            <ChangePasswordForm
              onSuccess={handleChangePasswordSuccess}
              onCancel={() => setShowChangePassword(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}