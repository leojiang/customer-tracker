'use client';

import { useState } from 'react';
import { Phone, Lock, LogIn } from 'lucide-react';
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
  
  const { login, isLoading } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.phone.trim() || !formData.password.trim()) {
      setError('Phone and password are required');
      return;
    }

    const result = await login(formData);
    if (!result.success) {
      setError(result.error || 'Login failed');
    }
  };

  const handleInputChange = (field: keyof LoginRequest, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (error) {
      setError(null);
    }
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
            {error && (
              <div className="bg-error-50 border border-error-200 rounded-lg p-3">
                <p className="text-error-600 text-sm">{error}</p>
              </div>
            )}

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