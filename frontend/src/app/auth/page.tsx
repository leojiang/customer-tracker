'use client';

import { useState } from 'react';
import LoginForm from '@/components/auth/LoginForm';
import RegisterForm from '@/components/auth/RegisterForm';
import RegistrationSuccess from '@/components/auth/RegistrationSuccess';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

type AuthView = 'login' | 'register' | 'success';

export default function AuthPage() {
  const [currentView, setCurrentView] = useState<AuthView>('login');
  const [registeredPhone, setRegisteredPhone] = useState('');
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      router.push('/');
    }
  }, [isAuthenticated, router]);

  const handleRegistrationSuccess = (phone: string) => {
    setRegisteredPhone(phone);
    setCurrentView('success');
  };

  const handleBackToLogin = () => {
    setCurrentView('login');
    setRegisteredPhone('');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-surface-50 flex items-center justify-center">
        <div className="loading-skeleton w-96 h-96 rounded-xl"></div>
      </div>
    );
  }

  if (isAuthenticated) {
    return null; // Will redirect
  }

  return (
    <div className="min-h-screen bg-surface-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-primary-500 rounded-2xl mx-auto flex items-center justify-center shadow-lg mb-4">
            <span className="text-white font-bold text-xl">CT</span>
          </div>
          <h1 className="text-headline-4 text-surface-900 mb-2">
            {currentView === 'success' ? 'Registration Complete!' : 'Customer Tracker'}
          </h1>
        </div>

        {currentView === 'login' && (
          <LoginForm onSwitchToRegister={() => setCurrentView('register')} />
        )}

        {currentView === 'register' && (
          <RegisterForm 
            onSwitchToLogin={() => setCurrentView('login')} 
            onRegistrationSuccess={handleRegistrationSuccess}
          />
        )}

        {currentView === 'success' && (
          <RegistrationSuccess 
            phone={registeredPhone}
            onBackToLogin={handleBackToLogin}
          />
        )}
      </div>
    </div>
  );
}