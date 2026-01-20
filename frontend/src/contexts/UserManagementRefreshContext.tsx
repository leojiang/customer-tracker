'use client';

import { createContext, useContext, useState, ReactNode } from 'react';

interface RefreshContextType {
  // Refresh functions for each tab
  refreshAllUsers: () => void;
  refreshUserApprovals: () => void;
  
  // Loading state
  isRefreshing: boolean;
  
  // Register refresh handlers from tab components
  registerAllUsersRefresh: (handler: () => void) => void;
  registerUserApprovalsRefresh: (handler: () => void) => void;
}

const RefreshContext = createContext<RefreshContextType | undefined>(undefined);

interface RefreshProviderProps {
  children: ReactNode;
}

export function UserManagementRefreshProvider({ children }: RefreshProviderProps) {
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [allUsersRefreshHandler, setAllUsersRefreshHandler] = useState<(() => void) | null>(null);
  const [userApprovalsRefreshHandler, setUserApprovalsRefreshHandler] = useState<(() => void) | null>(null);

  const refreshAllUsers = async () => {
    if (isRefreshing || !allUsersRefreshHandler) {
      return;
    }
    
    setIsRefreshing(true);
    try {
      await allUsersRefreshHandler();
    } catch (error) {
      console.error('Error refreshing all users:', error);
    } finally {
      setIsRefreshing(false);
    }
  };

  const refreshUserApprovals = async () => {
    if (isRefreshing || !userApprovalsRefreshHandler) {
      return;
    }
    
    setIsRefreshing(true);
    try {
      await userApprovalsRefreshHandler();
    } catch (error) {
      console.error('Error refreshing user approvals:', error);
    } finally {
      setIsRefreshing(false);
    }
  };

  const registerAllUsersRefresh = (handler: () => void) => {
    setAllUsersRefreshHandler(() => handler);
  };

  const registerUserApprovalsRefresh = (handler: () => void) => {
    setUserApprovalsRefreshHandler(() => handler);
  };

  const value: RefreshContextType = {
    refreshAllUsers,
    refreshUserApprovals,
    isRefreshing,
    registerAllUsersRefresh,
    registerUserApprovalsRefresh,
  };

  return (
    <RefreshContext.Provider value={value}>
      {children}
    </RefreshContext.Provider>
  );
}

export function useUserManagementRefresh() {
  const context = useContext(RefreshContext);
  if (context === undefined) {
    throw new Error('useUserManagementRefresh must be used within a UserManagementRefreshProvider');
  }
  return context;
}