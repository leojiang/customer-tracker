'use client';

import { useState, useEffect } from 'react';
import { Customer } from '@/types/customer';
import CustomerList from '@/components/customers/CustomerList';
import CustomerDetail from '@/components/customers/CustomerDetail';
import CustomerForm from '@/components/customers/CustomerForm';
import SettingsModal from '@/components/ui/SettingsModal';

import AuthGuard from '@/components/auth/AuthGuard';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { User, Shield, BarChart3, UserCheck, Settings, Users, Upload, Zap } from 'lucide-react';
import AdminDashboard from '@/app/dashboard/admin/page';
import UserManagementPage from '@/app/dashboard/admin/user-management/page';
import SalesDashboardInline from '@/components/dashboard/SalesDashboardInline';
import BatchImportExportPage from '@/app/batch-import-export/page';
import LandingPage from '@/components/landing/LandingPage';

type View = 'landing' | 'list' | 'detail' | 'create' | 'dashboard' | 'user-approvals' | 'batch-import-export';

export default function HomePage() {
  const [currentView, setCurrentView] = useState<View>('landing');
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [showSettings, setShowSettings] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const { t } = useLanguage();

  const handleCustomerSelect = (customer: Customer) => {
    setSelectedCustomer(customer);
    setCurrentView('detail');
  };

  const handleCreateCustomer = () => {
    // Prevent CUSTOMER_AGENT from accessing create view
    if (user?.role === 'CUSTOMER_AGENT') {
      return;
    }
    setCurrentView('create');
  };

  const handleBack = () => {
    setSelectedCustomer(null);
    setCurrentView('list');
  };

  const handleFormClose = () => {
    setCurrentView('list');
  };

  const handleFormSuccess = () => {
    setCurrentView('list');
    setRefreshTrigger(prev => prev + 1);
  };

  const handleNavigation = (view: View) => {
    // Prevent CUSTOMER_AGENT and OFFICER from accessing dashboard
    if (view === 'dashboard' && user?.role !== 'ADMIN') {
      return;
    }
    // Prevent CUSTOMER_AGENT from accessing user management
    if (user?.role === 'CUSTOMER_AGENT' && view === 'user-approvals') {
      return;
    }
    setCurrentView(view);
    setSelectedCustomer(null);
  };

  // Redirect users away from restricted views
  useEffect(() => {
    // Redirect non-ADMIN users away from dashboard
    if (currentView === 'dashboard' && user?.role !== 'ADMIN') {
      setCurrentView('landing');
    }
    // Redirect CUSTOMER_AGENT users away from user management
    if (user?.role === 'CUSTOMER_AGENT' && currentView === 'user-approvals') {
      setCurrentView('landing');
    }
  }, [user, currentView]);

  return (
    <AuthGuard>
      <div className="min-h-screen bg-surface-50 flex">
        {/* Left Sidebar */}
        <aside
          className={`bg-white shadow-md-2 fixed h-full z-50 flex flex-col transition-all duration-300 ease-in-out ${
            isSidebarCollapsed ? 'w-20' : 'w-64'
          }`}
        >
          {/* Edge click button */}
          <button
            onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
            className="absolute right-0 top-0 h-full w-2 transform translate-x-full bg-transparent hover:bg-gray-200 transition-colors cursor-col-resize"
            aria-label="Toggle sidebar"
          />

          {/* Logo and Brand */}
          <div
            className={`p-6 border-b border-surface-200 ${isSidebarCollapsed ? 'flex justify-center' : ''} cursor-pointer hover:bg-surface-50 transition-colors`}
            onClick={() => setCurrentView('landing')}
          >
            <div className={`flex items-center ${isSidebarCollapsed ? '' : 'gap-3'}`}>
              <div className="w-10 h-10 bg-primary-500 rounded-xl flex items-center justify-center shadow-md-2">
                <span className="text-white font-bold text-lg">CT</span>
              </div>
              {!isSidebarCollapsed && (
                <div>
                  <h1 className="text-headline-5 text-surface-900">{t('app.customerTracker')}</h1>
                </div>
              )}
            </div>
          </div>

          {/* Navigation Menu */}
          <nav className="flex-1 p-4 space-y-2">
            <button
              onClick={() => handleNavigation('landing')}
              className={`w-full flex items-center rounded-lg transition-colors ${
                isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
              } ${
                currentView === 'landing'
                  ? 'bg-primary-100 text-primary-700 border border-primary-200'
                  : 'text-surface-700 hover:bg-surface-100'
              }`}
            >
              <Zap size={20} />
              {!isSidebarCollapsed && <span className="font-medium">{t('landing.quickActions')}</span>}
            </button>

            <button
              onClick={() => handleNavigation('list')}
              className={`w-full flex items-center rounded-lg transition-colors ${
                isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
              } ${
                currentView === 'list'
                  ? 'bg-primary-100 text-primary-700 border border-primary-200'
                  : 'text-surface-700 hover:bg-surface-100'
              }`}
            >
              <Users size={20} />
              {!isSidebarCollapsed && <span className="font-medium">{t('nav.customers')}</span>}
            </button>

            {(user?.role === 'ADMIN' || user?.role === 'OFFICER') && (
              <button
                onClick={() => handleNavigation('batch-import-export')}
                className={`w-full flex items-center rounded-lg transition-colors ${
                  isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
                } ${
                  currentView === 'batch-import-export'
                    ? 'bg-primary-100 text-primary-700 border border-primary-200'
                    : 'text-surface-700 hover:bg-surface-100'
                }`}
              >
                <Upload size={20} />
                {!isSidebarCollapsed && <span className="font-medium">{t('nav.batchImport')}</span>}
              </button>
            )}

            {user?.role === 'ADMIN' && (
              <button
                onClick={() => handleNavigation('dashboard')}
                className={`w-full flex items-center rounded-lg transition-colors ${
                  isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
                } ${
                  currentView === 'dashboard'
                    ? 'bg-primary-100 text-primary-700 border border-primary-200'
                    : 'text-surface-700 hover:bg-surface-100'
                }`}
              >
                <BarChart3 size={20} />
                {!isSidebarCollapsed && <span className="font-medium">{t('nav.dashboard')}</span>}
              </button>
            )}

            {user?.role === 'ADMIN' && (
              <button
                onClick={() => handleNavigation('user-approvals')}
                className={`w-full flex items-center rounded-lg transition-colors ${
                  isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
                } ${
                  currentView === 'user-approvals'
                    ? 'bg-primary-100 text-primary-700 border border-primary-200'
                    : 'text-surface-700 hover:bg-surface-100'
                }`}
              >
                <UserCheck size={20} />
                {!isSidebarCollapsed && <span className="font-medium">{t('nav.userManagement')}</span>}
              </button>
            )}
          </nav>

          {/* User Info Section at Bottom */}
          <div className="p-4 border-t border-surface-200">
            <button
              onClick={() => setShowSettings(true)}
              className={`w-full flex items-center bg-surface-100 rounded-lg hover:bg-surface-200 transition-colors ${
                isSidebarCollapsed ? 'justify-center px-4 py-3' : 'gap-3 px-4 py-3'
              }`}
            >
              {user?.role === 'ADMIN' ? (
                <Shield size={20} className="text-primary-600" />
              ) : (
                <User size={20} className="text-surface-600" />
              )}
              {!isSidebarCollapsed && (
                <>
                  <div className="flex-1 text-left">
                    <div className="text-sm font-medium text-surface-700">
                      {user?.phone}
                    </div>
                    {user?.role === 'ADMIN' && (
                      <div className="text-xs text-surface-500">{t('app.admin')}</div>
                    )}
                  </div>
                  <Settings size={16} className="text-surface-500" />
                </>
              )}
            </button>
          </div>
        </aside>

        {/* Main Content Area */}
        <div className={`flex-1 transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-20' : 'ml-64'} h-screen overflow-hidden`}>
          <main className="h-full overflow-hidden">
            {currentView === 'landing' && (
              <LandingPage
                onNavigateToCustomers={() => handleNavigation('list')}
                onNavigateToDashboard={() => handleNavigation('dashboard')}
                onNavigateToBatchImport={() => handleNavigation('batch-import-export')}
                onNavigateToUserManagement={() => handleNavigation('user-approvals')}
              />
            )}

            {currentView === 'list' && (
              <div className="h-full flex flex-col">
                <CustomerList
                  key={refreshTrigger}
                  onCustomerSelect={handleCustomerSelect}
                  onCreateCustomer={handleCreateCustomer}
                />
              </div>
            )}

            {currentView === 'detail' && selectedCustomer && (
              <div className="h-full overflow-y-auto">
                <CustomerDetail
                  customerId={selectedCustomer.id}
                  onBack={handleBack}
                />
              </div>
            )}

            {currentView === 'create' && (
              <CustomerForm
                onClose={handleFormClose}
                onSuccess={handleFormSuccess}
              />
            )}

            {currentView === 'dashboard' && user?.role === 'ADMIN' && (
              <div className="h-full overflow-y-auto">
                <AdminDashboard />
              </div>
            )}

            {currentView === 'dashboard' && user?.role !== 'ADMIN' && (
              <div className="h-full overflow-y-auto">
                {/* Import and render Sales Dashboard component without navigation */}
                <SalesDashboardInline onNavigateToCustomers={() => handleNavigation('list')} />
              </div>
            )}

            {currentView === 'user-approvals' && (
              <div className="h-full overflow-y-auto">
                <UserManagementPage />
              </div>
            )}

            {currentView === 'batch-import-export' && (
              <div className="h-full overflow-y-auto">
                <BatchImportExportPage />
              </div>
            )}
          </main>
        </div>
      
      {/* Settings Modal */}
      <SettingsModal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        onLogout={logout}
      />
      </div>
    </AuthGuard>
  );
}
