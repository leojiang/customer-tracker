'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Sales, LoginRequest, RegisterRequest } from '@/types/auth';
import { authApi } from '@/lib/api';

interface AuthContextType {
  user: Sales | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  mustChangePassword: boolean;
  setMustChangePassword: (value: boolean) => void;
  login: (credentials: LoginRequest) => Promise<{ success: boolean; error?: string; mustChangePassword?: boolean }>;
  register: (data: RegisterRequest) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<Sales | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [mustChangePassword, setMustChangePassword] = useState(false);

  const isAuthenticated = !!user && !!token;
  

  // Check for existing token on app start
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const storedToken = localStorage.getItem('auth_token');
        if (storedToken) {
          const response = await authApi.validateToken({ token: storedToken });
          if (response.token && response.phone && response.role) {
            const userData: Sales = {
              phone: response.phone,
              role: response.role,
            };
            setUser(userData);
            setToken(storedToken);
            localStorage.setItem('user_data', JSON.stringify(userData));
          } else {
            // Invalid token, clear it
            authApi.logout();
          }
        }
      } catch (error) {
        // Token validation failed, clear it
        authApi.logout();
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (credentials: LoginRequest): Promise<{ success: boolean; error?: string; mustChangePassword?: boolean }> => {
    try {
      // Do NOT use global isLoading for login operations - use local state in components instead
      const response = await authApi.login(credentials);

      if (response.error) {
        return { success: false, error: response.error };
      }

      if (response.token && response.phone && response.role) {
        const userData: Sales = {
          phone: response.phone,
          role: response.role,
        };

        setUser(userData);
        setToken(response.token);
        setMustChangePassword(response.mustChangePassword || false);
        localStorage.setItem('auth_token', response.token);
        localStorage.setItem('user_data', JSON.stringify(userData));

        return { success: true, mustChangePassword: response.mustChangePassword };
      }

      return { success: false, error: 'error.invalidResponse' };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'auth.loginFailed'
      };
    }
  };

  const register = async (data: RegisterRequest): Promise<{ success: boolean; error?: string }> => {
    try {
      // Do NOT use global isLoading for register operations - use local state in components instead
      const response = await authApi.register(data);
      
      if (response.error) {
        return { success: false, error: response.error };
      }

      // Handle successful registration (either immediate approval or pending status)
      if (response.phone && response.role) {
        // If token is provided, user is immediately approved (admin users)
        if (response.token) {
          const userData: Sales = {
            phone: response.phone,
            role: response.role,
          };
          
          setUser(userData);
          setToken(response.token);
          localStorage.setItem('auth_token', response.token);
          localStorage.setItem('user_data', JSON.stringify(userData));
        }
        // If no token but has phone/role, user is pending approval
        return { success: true };
      }
      
      return { success: false, error: 'error.invalidResponse' };
    } catch (error) {
      return { 
        success: false, 
        error: error instanceof Error ? error.message : 'auth.registerFailed' 
      };
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    authApi.logout();
    // Clear customer list filters from localStorage
    try {
      localStorage.removeItem('customerListFilters');
    } catch (error) {
      console.error('Error clearing filters from localStorage:', error);
    }
  };

  const value: AuthContextType = {
    user,
    token,
    isLoading,
    isAuthenticated,
    mustChangePassword,
    setMustChangePassword,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}