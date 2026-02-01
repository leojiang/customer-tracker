'use client';

import { useState, useEffect } from 'react';
import { Phone, Lock, LogIn, Clock, XCircle, AlertCircle, LogOut } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { LoginRequest } from '@/types/auth';
import ChangePasswordForm from '@/components/ui/ChangePasswordForm';

interface LoginFormProps {
  onSwitchToRegister: () => void;
}

export default function LoginForm({ onSwitchToRegister }: LoginFormProps) {

  const [formData, setFormData] = useState<LoginRequest>({
    phone: '',
    password: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const { login, mustChangePassword } = useAuth();
  const { t } = useLanguage();

  // Check for session conflict on component mount
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const sessionConflict = sessionStorage.getItem('session_conflict');
      if (sessionConflict === 'true') {
        // Show session conflict message
        setError(t('error.sessionConflict'));
        // Clear the flag so it doesn't show again on refresh
        sessionStorage.removeItem('session_conflict');
      }
    }
  }, [t]);


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();


    if (!formData.phone.trim() || !formData.password.trim()) {
      setError(t('error.phonePasswordRequired'));
      return;
    }

    setIsLoading(true);
    setError(null); // Clear any previous errors

    try {
      const result = await login(formData);

      if (!result.success) {
        // Parse error message and provide user-friendly feedback
        const errorMessage = result.error || 'auth.loginFailed';
        // Always try to translate the error message
        setError(t(errorMessage));
      }
      // Note: mustChangePassword is now handled by AuthContext and AuthPage
    } catch (error) {
      // Handle network errors or other unexpected issues
      if (error instanceof Error) {
        if (error.message.includes('Network error')) {
          setError(t('error.networkError'));
        } else if (error.message.includes('403') || error.message.includes('401')) {
          setError(t('error.incorrectCredentials'));
        } else {
          setError(t('error.unexpectedError'));
        }
      } else {
        setError(t('error.unexpectedError'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleInputChange = (field: keyof LoginRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear error when user starts typing after seeing invalid credentials error
    if (error && (error === t('error.incorrectCredentials') || error.includes('Incorrect phone number or password') || error.includes('手机号码或密码错误'))) {
      setError(null);
    }
  };

  const renderErrorMessage = () => {
    if (!error) {
      return null;
    }

    let errorType = 'error';
    let icon = <AlertCircle className="w-5 h-5" />;
    let bgClass = 'bg-red-50 border-red-200';
    let textClass = 'text-red-700';
    let iconClass = 'text-red-500';
    let title = t('error.loginFailed');

    // Enhanced error type detection based on translation keys and messages
    if (error === t('error.sessionConflict') || error.includes('logged out') || error.includes('登录失效')) {
      errorType = 'session-conflict';
      icon = <LogOut className="w-5 h-5" />;
      bgClass = 'bg-blue-50 border-blue-200';
      textClass = 'text-blue-700';
      iconClass = 'text-blue-500';
      title = t('error.sessionConflictTitle');
    } else if (error === t('error.incorrectCredentials') || error.includes('Incorrect phone number or password') || error.includes('check your credentials') || error.includes('手机号码或密码错误')) {
      errorType = 'invalid-credentials';
      icon = <AlertCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      iconClass = 'text-red-500';
      title = t('error.invalidCredentials');
    } else if (error === t('error.accountPending') || error.includes('pending approval') || error.includes('等待审批')) {
      errorType = 'pending';
      icon = <Clock className="w-5 h-5" />;
      bgClass = 'bg-yellow-50 border-yellow-200';
      textClass = 'text-yellow-700';
      iconClass = 'text-yellow-500';
      title = t('error.accountPending');
    } else if (error === t('error.accountDenied') || error.includes('access has been denied') || error.includes('访问被拒绝')) {
      errorType = 'rejected';
      icon = <XCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      iconClass = 'text-red-500';
      title = t('error.accountDenied');
    } else if (error === t('error.networkError') || error.includes('Unable to connect') || error.includes('Network error') || error.includes('无法连接')) {
      errorType = 'network';
      icon = <AlertCircle className="w-5 h-5" />;
      bgClass = 'bg-orange-50 border-orange-200';
      textClass = 'text-orange-700';
      iconClass = 'text-orange-500';
      title = t('error.connectionError');
    }

    return (
      <div className={`${bgClass} border rounded-lg p-4 mb-4`}>
        <div className="flex items-start gap-3">
          <div className={iconClass}>
            {icon}
          </div>
          <div className="flex-1">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <h4 className={`font-semibold ${textClass} mb-1`}>{title}</h4>
                <p className={`text-sm ${textClass.replace('700', '600')}`}>
                  {error}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setError(null)}
                className={`${textClass.replace('700', '400')} hover:${textClass.replace('700', '600')} ml-2 p-1 rounded-md hover:bg-current hover:bg-opacity-10`}
                aria-label={t('error.dismissMessage')}
              >
                ×
              </button>
            </div>
            
            {/* Contextual help based on error type */}
            {errorType === 'session-conflict' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">{t('error.whatHappened')}</p>
                <p>{t('error.sessionConflictExplanation')}</p>
                <p className="mt-2">{t('error.sessionCanRelogin')}</p>
              </div>
            )}

            {errorType === 'invalid-credentials' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">{t('error.tips')}</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>{t('error.checkPhoneFormat')}</li>
                  <li>{t('error.ensurePassword')}</li>
                  <li>{t('error.capsLock')}</li>
                </ul>
              </div>
            )}
            
            {errorType === 'pending' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">{t('error.whatToDo')}</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>{t('error.waitApproval')}</li>
                  <li>{t('error.contactAdmin')}</li>
                  <li>{t('error.tryAgainLater')}</li>
                </ul>
              </div>
            )}
            
            {errorType === 'rejected' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">{t('error.needHelp')}</p>
                <p>{t('error.contactSystemAdmin')}</p>
              </div>
            )}
            
            {errorType === 'network' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">{t('error.trySteps')}</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>{t('error.checkConnection')}</li>
                  <li>{t('error.refreshPage')}</li>
                  <li>{t('error.contactSupport')}</li>
                </ul>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  const handlePasswordChangeSuccess = () => {
    // Password changed successfully - will be redirected automatically by AuthPage
    // since mustChangePassword will be false after successful change
  };

  return (
    <div className="w-full max-w-md">
      {mustChangePassword ? (
        <ChangePasswordForm
          forcedPasswordChange={true}
          currentPasswordValue={formData.password}
          onSuccess={handlePasswordChangeSuccess}
        />
      ) : (
        <div className="card-elevated">
        <div className="card-header text-center">
          <h1 className="text-headline-4">{t('auth.welcomeBack')}</h1>
          <p className="text-body-2 mt-2">{t('auth.signInToAccount')}</p>
        </div>
        
        <div className="card-content">
          <form onSubmit={handleSubmit} className="space-y-6" noValidate>
            {renderErrorMessage()}

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
                <Lock size={18} className="text-surface-500" />
                {t('auth.password')}
              </label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                className="input-field focus-ring"
                placeholder={t('auth.enterPassword')}
                required
              />
            </div>

            <button 
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full flex items-center justify-center gap-3"
            >
              <LogIn size={18} />
              {isLoading ? t('auth.signingIn') : t('auth.signIn')}
            </button>
          </form>

          <div className="divider"></div>
          
          <div className="text-center">
            <p className="text-body-2">
              {t('auth.dontHaveAccount')}{' '}
              <button
                type="button"
                onClick={onSwitchToRegister}
                className="text-primary-600 hover:text-primary-700 font-medium focus:outline-none focus:underline"
              >
                {t('auth.createAccount')}
              </button>
            </p>
          </div>
        </div>
      </div>
      )}
    </div>
  );
}