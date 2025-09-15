'use client';

import { CheckCircle, Clock, ArrowLeft } from 'lucide-react';
import { useLanguage } from '@/contexts/LanguageContext';

interface RegistrationSuccessProps {
  phone: string;
  onBackToLogin: () => void;
}

export default function RegistrationSuccess({ phone, onBackToLogin }: RegistrationSuccessProps) {
  const { t } = useLanguage();
  return (
    <div className="w-full max-w-md">
      <div className="card-elevated text-center">
        <div className="card-content py-8">
          {/* Success Icon */}
          <div className="flex justify-center mb-6">
            <div className="relative">
              <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center">
                <CheckCircle className="w-10 h-10 text-green-600" />
              </div>
              <div className="absolute -top-1 -right-1 w-8 h-8 bg-yellow-100 rounded-full flex items-center justify-center">
                <Clock className="w-4 h-4 text-yellow-600" />
              </div>
            </div>
          </div>

          {/* Success Message */}
          <h1 className="text-2xl font-bold text-gray-900 mb-6">
            {t('register.success.title')}
          </h1>
          
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
            <div className="flex items-center gap-3">
              <CheckCircle className="w-6 h-6 text-green-600 flex-shrink-0" />
              <div>
                <p className="text-green-800 font-medium">{t('register.success.message')}</p>
                <p className="text-green-700 text-sm mt-1">
                  {t('auth.phone')}: <strong>{phone}</strong>
                </p>
              </div>
            </div>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <div className="flex items-center gap-3">
              <Clock className="w-6 h-6 text-yellow-600 flex-shrink-0" />
              <div>
                <p className="text-yellow-800 font-medium">{t('register.success.adminReview')}</p>
                <p className="text-yellow-700 text-sm mt-1">
                  {t('register.success.notification')}
                </p>
              </div>
            </div>
          </div>

          {/* Back to Login Button */}
          <div className="mt-8">
            <button
              onClick={onBackToLogin}
              className="btn-primary w-full flex items-center justify-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              {t('register.success.backToLogin')}
            </button>
          </div>

          {/* Contact Info */}
          <div className="mt-6 pt-4 border-t border-gray-200">
            <p className="text-xs text-gray-500">
              {t('error.needHelp')} {t('error.contactSystemAdmin')}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}