'use client';

import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { useRouter } from 'next/navigation';
import { 
  ArrowLeft, 
  Users, 
  BarChart3, 
  RefreshCw, 
  Menu, 
  X,
  Settings
} from 'lucide-react';
import LanguageSwitcher from '@/components/ui/LanguageSwitcher';
import SettingsModal from '@/components/ui/SettingsModal';

interface DashboardLayoutProps {
  children: React.ReactNode;
  title: string;
  description?: string;
  onRefresh?: () => void;
  refreshing?: boolean;
  showBackButton?: boolean;
  actions?: React.ReactNode;
}

export default function DashboardLayout({
  children,
  title,
  description,
  onRefresh,
  refreshing = false,
  showBackButton = true,
  actions
}: DashboardLayoutProps) {
  const { user, logout } = useAuth();
  const { t } = useLanguage();
  const router = useRouter();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [showSettings, setShowSettings] = useState(false);

  const navigation = [
    {
      name: t('nav.dashboard'),
      href: '/dashboard',
      icon: BarChart3,
      current: true
    },
    {
      name: t('nav.customers'),
      href: '/',
      icon: Users,
      current: false
    },
    {
      name: t('nav.settings'),
      href: '/settings',
      icon: Settings,
      current: false
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile sidebar */}
      <div className={`fixed inset-0 z-40 md:hidden ${sidebarOpen ? 'block' : 'hidden'}`}>
        <div className="fixed inset-0 bg-gray-600 bg-opacity-75" onClick={() => setSidebarOpen(false)} />
        <div className="relative flex w-full max-w-xs flex-1 flex-col bg-white">
          <div className="absolute top-0 right-0 -mr-12 pt-2">
            <button
              type="button"
              className="ml-1 flex h-10 w-10 items-center justify-center rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
              onClick={() => setSidebarOpen(false)}
            >
              <X className="h-6 w-6 text-white" />
            </button>
          </div>
          
          <div className="flex flex-shrink-0 items-center px-4 py-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">CT</span>
              </div>
              <span className="text-lg font-semibold">{t('nav.dashboard')}</span>
            </div>
          </div>
          
          <div className="mt-5 flex-1 h-0 overflow-y-auto">
            <nav className="px-2 space-y-1">
              {navigation.map((item) => (
                <a
                  key={item.name}
                  href={item.href}
                  className={`group flex items-center px-2 py-2 text-base font-medium rounded-md ${
                    item.current
                      ? 'bg-gray-100 text-gray-900'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`}
                >
                  <item.icon className="mr-4 h-5 w-5 text-gray-400 group-hover:text-gray-500" />
                  {item.name}
                </a>
              ))}
              
            </nav>
          </div>
        </div>
      </div>

      {/* Desktop sidebar */}
      <div className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0">
        <div className="flex-1 flex flex-col min-h-0 bg-white border-r border-gray-200">
          <div className="flex-1 flex flex-col pt-5 pb-4 overflow-y-auto">
            <div className="flex items-center flex-shrink-0 px-4">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
                  <span className="text-white font-bold text-sm">CT</span>
                </div>
                <span className="text-lg font-semibold">Dashboard</span>
              </div>
            </div>
            <nav className="mt-5 flex-1 px-2 space-y-1">
              {navigation.map((item) => (
                <a
                  key={item.name}
                  href={item.href}
                  className={`group flex items-center px-2 py-2 text-sm font-medium rounded-md ${
                    item.current
                      ? 'bg-gray-100 text-gray-900'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`}
                >
                  <item.icon className="mr-3 h-5 w-5 text-gray-400 group-hover:text-gray-500" />
                  {item.name}
                </a>
              ))}
            </nav>
          </div>
          <div className="flex-shrink-0 flex border-t border-gray-200 p-4">
            <div className="flex-shrink-0 w-full group block">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className="inline-flex items-center justify-center h-8 w-8 rounded-full bg-gray-500">
                    <span className="text-sm font-medium leading-none text-white">
                      {user?.phone?.charAt(0) || 'U'}
                    </span>
                  </div>
                  <div className="ml-3">
                    <p className="text-sm font-medium text-gray-700 group-hover:text-gray-900">
                      {user?.phone}
                    </p>
                    <p className="text-xs font-medium text-gray-500 group-hover:text-gray-700">
                      {user?.role}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowSettings(true)}
                  className="text-gray-400 hover:text-gray-500"
                >
                  <Settings className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="md:pl-64 flex flex-col flex-1">
        {/* Header */}
        <div className="sticky top-0 z-10 bg-white shadow-sm border-b border-gray-200">
          <div className="px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center">
                <button
                  type="button"
                  className="border-r border-gray-200 pr-4 text-gray-500 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500 md:hidden"
                  onClick={() => setSidebarOpen(true)}
                >
                  <Menu className="h-6 w-6" />
                </button>
                
                <div className="flex items-center gap-4 ml-4 md:ml-0">
                  {showBackButton && (
                    <button
                      onClick={() => router.push('/')}
                      className="inline-flex items-center text-sm text-gray-600 hover:text-gray-900"
                    >
                      <ArrowLeft size={16} className="mr-1" />
                      <span className="hidden sm:inline">Back to Customers</span>
                    </button>
                  )}
                  <div>
                    <h1 className="text-lg font-semibold text-gray-900">{title}</h1>
                    {description && (
                      <p className="text-sm text-gray-500">{description}</p>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                {/* Language Switcher */}
                <LanguageSwitcher />
                
                {onRefresh && (
                  <button
                    onClick={onRefresh}
                    disabled={refreshing}
                    className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <RefreshCw size={16} className={`mr-1 ${refreshing ? 'animate-spin' : ''}`} />
                    <span className="hidden sm:inline">{refreshing ? t('nav.refreshing') : t('nav.refresh')}</span>
                  </button>
                )}
                
                <button
                  onClick={() => router.push('/')}
                  className="inline-flex items-center rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
                >
                  <Users size={16} className="mr-1" />
                  <span className="hidden sm:inline">{t('nav.customers')}</span>
                </button>
                
                {actions}
              </div>
            </div>
          </div>
        </div>

        {/* Page content */}
        <main className="flex-1">
          <div className="py-6">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
              {children}
            </div>
          </div>
        </main>
      </div>
      
      {/* Settings Modal */}
      <SettingsModal 
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        onLogout={logout}
      />
    </div>
  );
}