'use client';

import { useState } from 'react';
import { Phone, Lock, UserPlus, Eye, EyeOff, Shield, User } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { RegisterRequest, SalesRole } from '@/types/auth';

interface RegisterFormProps {
  onSwitchToLogin: () => void;
  onRegistrationSuccess: (phone: string) => void;
}

export default function RegisterForm({ onSwitchToLogin, onRegistrationSuccess }: RegisterFormProps) {
  const [formData, setFormData] = useState<RegisterRequest>({
    phone: '',
    name: '',
    password: '',
    confirmPassword: '',
    role: SalesRole.CUSTOMER_AGENT, // Default to Customer Agent
  });
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const { register, isLoading } = useAuth();
  const { t } = useLanguage();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!formData.phone.trim() || !formData.name.trim() || !formData.password.trim() || !formData.confirmPassword.trim()) {
      setError(t('register.allFieldsRequired'));
      return;
    }

    if (!formData.role) {
      setError(t('register.selectRole'));
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError(t('register.passwordsDontMatch'));
      return;
    }

    if (formData.password.length < 6) {
      setError(t('register.passwordTooShort'));
      return;
    }

    try {
      const result = await register(formData);
      if (!result.success) {
        // Always try to translate the error message
        const errorMessage = result.error || 'auth.registerFailed';
        setError(t(errorMessage));
      } else {
        // Navigate to success page
        onRegistrationSuccess(formData.phone);
      }
    } catch (error) {
      setError(t('auth.registerFailed'));
    }
  };

  const handleInputChange = (field: keyof RegisterRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear error when user starts typing
    if (error) {
      setError(null);
    }
  };

  return (
    <div className="w-full max-w-md">
      <div className="card-elevated">
        <div className="card-header text-center">
          <h1 className="text-headline-4">{t('register.createAccount')}</h1>
          <p className="text-body-2 mt-2">{t('register.joinPlatform')}</p>
        </div>
        
        <div className="card-content">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <div className="flex items-start justify-between">
                  <p className="text-red-600 text-sm flex-1">{error}</p>
                  <button
                    type="button"
                    onClick={() => setError(null)}
                    className="text-red-400 hover:text-red-600 ml-2"
                  >
                    ×
                  </button>
                </div>
              </div>
            )}


            <div>
              <label className="input-label flex items-center gap-2">
                <User size={18} className="text-surface-500" />
                {t('customer.name')}
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => handleInputChange('name', e.target.value)}
                className="input-field focus-ring"
                placeholder={t('customers.form.name')}
                required
              />
            </div>

            <div>
              <label className="input-label flex items-center gap-2">
                <Phone size={18} className="text-surface-500" />
                {t('auth.phoneNumber')}
              </label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => handleInputChange('phone', e.target.value)}
                className="input-field focus-ring"
                placeholder={t('auth.enterPhone')}
                required
              />
            </div>

            <div>
              <label className="input-label flex items-center gap-2">
                <Shield size={18} className="text-surface-500" />
                {t('register.role')}
              </label>
              <select
                value={formData.role}
                onChange={(e) => handleInputChange('role', e.target.value)}
                className="input-field focus-ring"
                required
              >
                <option value="">{t('register.selectRole')}</option>
                <option value={SalesRole.OFFICER}>{t('role.officer')}</option>
                <option value={SalesRole.CUSTOMER_AGENT}>{t('role.customerAgent')}</option>
              </select>
              <p className="text-xs text-surface-500 mt-1">
                {t('register.selectRole')}
              </p>
            </div>

            <div>
              <label className="input-label flex items-center gap-2">
                <Lock size={18} className="text-surface-500" />
                {t('auth.password')}
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={(e) => handleInputChange('password', e.target.value)}
                  className="input-field focus-ring pr-12"
                  placeholder={t('register.createPassword')}
                  required
                  minLength={6}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-surface-500 hover:text-surface-700 focus:outline-none"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              <p className="text-xs text-surface-500 mt-1">{t('register.minimumChars')}</p>
            </div>

            <div>
              <label className="input-label flex items-center gap-2">
                <Lock size={18} className="text-surface-500" />
                {t('register.confirmPassword')}
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={formData.confirmPassword}
                  onChange={(e) => handleInputChange('confirmPassword', e.target.value)}
                  className="input-field focus-ring pr-12"
                  placeholder={t('register.enterConfirmPassword')}
                  required
                  minLength={6}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-surface-500 hover:text-surface-700 focus:outline-none"
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button 
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full flex items-center justify-center gap-3"
            >
              <UserPlus size={18} />
              {isLoading ? t('register.creating') : t('register.createAccount')}
            </button>
          </form>

          <div className="divider"></div>
          
          <div className="text-center">
            <p className="text-body-2">
              {t('register.alreadyHaveAccount')}{' '}
              <button
                type="button"
                onClick={onSwitchToLogin}
                className="text-primary-600 hover:text-primary-700 font-medium focus:outline-none focus:underline"
              >
                {t('register.signIn')}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}