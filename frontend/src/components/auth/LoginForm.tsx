'use client';

import { useState } from 'react';
import { Phone, Lock, LogIn, Clock, XCircle, AlertCircle } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { LoginRequest } from '@/types/auth';

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
  
  const { login } = useAuth();


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    
    if (!formData.phone.trim() || !formData.password.trim()) {
      setError('Phone and password are required');
      return;
    }

    setIsLoading(true);
    setError(null); // Clear any previous errors
    
    try {
      const result = await login(formData);
      
      if (!result.success) {
        // Parse error message and provide user-friendly feedback
        const errorMessage = result.error || 'Login failed';
        // Display backend messages directly - they are now user-friendly
        if (errorMessage.includes('Invalid credentials')) {
          setError('Incorrect phone number or password. Please check your credentials and try again.');
        } else {
          // For all other errors (including pending/rejected), use the backend message directly
          setError(errorMessage);
        }
      }
    } catch (error) {
      // Handle network errors or other unexpected issues
      if (error instanceof Error) {
        if (error.message.includes('Network error')) {
          setError('Unable to connect to the server. Please check your internet connection and try again.');
        } else if (error.message.includes('403') || error.message.includes('401')) {
          setError('Incorrect phone number or password. Please check your credentials and try again.');
        } else {
          setError('An unexpected error occurred. Please try again.');
        }
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleInputChange = (field: keyof LoginRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear error when user starts typing after seeing invalid credentials error
    if (error && error.includes('Incorrect phone number or password')) {
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
    let title = 'Login Failed';

    // Enhanced error type detection based on improved error messages
    if (error.includes('Incorrect phone number or password') || error.includes('check your credentials')) {
      errorType = 'invalid-credentials';
      icon = <AlertCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      iconClass = 'text-red-500';
      title = 'Invalid Credentials';
    } else if (error.includes('pending approval')) {
      errorType = 'pending';
      icon = <Clock className="w-5 h-5" />;
      bgClass = 'bg-yellow-50 border-yellow-200';
      textClass = 'text-yellow-700';
      iconClass = 'text-yellow-500';
      title = 'Account Pending Approval';
    } else if (error.includes('access has been denied')) {
      errorType = 'rejected';
      icon = <XCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      iconClass = 'text-red-500';
      title = 'Account Access Denied';
    } else if (error.includes('Unable to connect') || error.includes('Network error')) {
      errorType = 'network';
      icon = <AlertCircle className="w-5 h-5" />;
      bgClass = 'bg-orange-50 border-orange-200';
      textClass = 'text-orange-700';
      iconClass = 'text-orange-500';
      title = 'Connection Error';
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
                aria-label="Dismiss error message"
              >
                ×
              </button>
            </div>
            
            {/* Contextual help based on error type */}
            {errorType === 'invalid-credentials' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">Tips:</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>Double-check your phone number format</li>
                  <li>Ensure your password is correct</li>
                  <li>Make sure Caps Lock is not enabled</li>
                </ul>
              </div>
            )}
            
            {errorType === 'pending' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">What to do next:</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>Please wait for admin approval</li>
                  <li>Contact admin if you need immediate access</li>
                  <li>Try logging in again later</li>
                </ul>
              </div>
            )}
            
            {errorType === 'rejected' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">Need help?</p>
                <p>Contact your system administrator to discuss your account status and request access.</p>
              </div>
            )}
            
            {errorType === 'network' && (
              <div className={`mt-3 text-sm ${textClass.replace('700', '600')}`}>
                <p className="font-medium mb-1">Try these steps:</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>Check your internet connection</li>
                  <li>Refresh the page and try again</li>
                  <li>Contact support if the problem persists</li>
                </ul>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="w-full max-w-md">
      <div className="card-elevated">
        <div className="card-header text-center">
          <h1 className="text-headline-4">Welcome Back</h1>
          <p className="text-body-2 mt-2">Sign in to your sales account</p>
        </div>
        
        <div className="card-content">
          <form onSubmit={handleSubmit} className="space-y-6" noValidate>
            {renderErrorMessage()}

            <div>
              <label className="input-label flex items-center gap-2">
                <Phone size={18} className="text-surface-500" />
                Phone Number
              </label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => handleInputChange('phone', e.target.value)}
                className="input-field focus-ring"
                placeholder="Enter your phone number"
                required
              />
            </div>

            <div>
              <label className="input-label flex items-center gap-2">
                <Lock size={18} className="text-surface-500" />
                Password
              </label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                className="input-field focus-ring"
                placeholder="Enter your password"
                required
              />
            </div>

            <button 
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full flex items-center justify-center gap-3"
            >
              <LogIn size={18} />
              {isLoading ? 'Signing In...' : 'Sign In'}
            </button>
          </form>

          <div className="divider"></div>
          
          <div className="text-center">
            <p className="text-body-2">
              Don&apos;t have an account?{' '}
              <button
                type="button"
                onClick={onSwitchToRegister}
                className="text-primary-600 hover:text-primary-700 font-medium focus:outline-none focus:underline"
              >
                Create Account
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}