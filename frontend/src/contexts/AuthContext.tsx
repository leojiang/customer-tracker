'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Sales, LoginRequest, RegisterRequest } from '@/types/auth';
import { authApi } from '@/lib/api';

interface AuthContextType {
  user: Sales | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<{ success: boolean; error?: string }>;
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

  const login = async (credentials: LoginRequest): Promise<{ success: boolean; error?: string }> => {
    try {
      setIsLoading(true);
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
        localStorage.setItem('auth_token', response.token);
        localStorage.setItem('user_data', JSON.stringify(userData));
        
        return { success: true };
      }
      
      return { success: false, error: 'Invalid response from server' };
    } catch (error) {
      return { 
        success: false, 
        error: error instanceof Error ? error.message : 'Login failed' 
      };
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<{ success: boolean; error?: string }> => {
    try {
      setIsLoading(true);
      const response = await authApi.register(data);
      
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
        localStorage.setItem('auth_token', response.token);
        localStorage.setItem('user_data', JSON.stringify(userData));
        
        return { success: true };
      }
      
      return { success: false, error: 'Invalid response from server' };
    } catch (error) {
      return { 
        success: false, 
        error: error instanceof Error ? error.message : 'Registration failed' 
      };
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    authApi.logout();
  };

  const value: AuthContextType = {
    user,
    token,
    isLoading,
    isAuthenticated,
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