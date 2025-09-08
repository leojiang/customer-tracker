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
  const [error, setError] = useState<string | null>('Test error message - this should be visible');
  const [isLoading, setIsLoading] = useState(false);
  
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    console.log('Login form submitted'); // Debug log
    
    if (!formData.phone.trim() || !formData.password.trim()) {
      setError('Phone and password are required');
      console.log('Validation error set'); // Debug log
      return false;
    }

    setIsLoading(true);
    setError(null); // Clear any previous errors
    
    try {
      console.log('Calling login API...'); // Debug log
      const result = await login(formData);
      console.log('Login result:', result); // Debug log
      
      if (!result.success) {
        // Show specific message for invalid credentials
        if (result.error && (result.error.includes('Invalid credentials') || result.error.includes('Invalid username') || result.error.includes('Invalid password'))) {
          console.log('Setting invalid credentials error'); // Debug log
          setError('Your username or password is not correct.');
        } else {
          console.log('Setting other error:', result.error); // Debug log
          setError(result.error || 'Login failed');
        }
      }
    } catch (error) {
      console.log('Login error caught:', error); // Debug log
      setError('Your username or password is not correct.');
    } finally {
      setIsLoading(false);
    }
    
    return false;
  };

  const handleInputChange = (field: keyof LoginRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Don't auto-clear errors - let user see them
  };

  const renderErrorMessage = () => {
    console.log('Rendering error message, error state:', error); // Debug log
    if (!error) {
      return null;
    }

    let errorType = 'error';
    let icon = <AlertCircle className="w-5 h-5" />;
    let bgClass = 'bg-red-50 border-red-200';
    let textClass = 'text-red-700';
    let title = 'Login Failed';

    if (error.includes('Your username or password is not correct')) {
      errorType = 'invalid-credentials';
      icon = <AlertCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      title = 'Login Failed';
    } else if (error.includes('pending approval') || error.includes('pending')) {
      errorType = 'pending';
      icon = <Clock className="w-5 h-5" />;
      bgClass = 'bg-yellow-50 border-yellow-200';
      textClass = 'text-yellow-700';
      title = 'Account Pending Approval';
    } else if (error.includes('rejected') || error.includes('denied')) {
      errorType = 'rejected';
      icon = <XCircle className="w-5 h-5" />;
      bgClass = 'bg-red-50 border-red-200';
      textClass = 'text-red-700';
      title = 'Account Access Denied';
    }

    return (
      <div className={`${bgClass} border rounded-lg p-4`}>
        <div className="flex items-start gap-3">
          <div className={textClass}>
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
                className={`${textClass.replace('700', '400')} hover:${textClass.replace('700', '600')} ml-2`}
              >
                ×
              </button>
            </div>
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
                <p>Contact your system administrator to discuss your account status.</p>
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
          <form onSubmit={handleSubmit} className="space-y-6">
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
              type="button"
              onClick={handleSubmit}
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