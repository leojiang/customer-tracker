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
import { User, Shield, BarChart3, UserCheck, Settings, Users } from 'lucide-react';
import AdminDashboard from '@/app/dashboard/admin/page';
import UserManagementPage from '@/app/dashboard/admin/user-management/page';
import SalesDashboardInline from '@/components/dashboard/SalesDashboardInline';

type View = 'list' | 'detail' | 'create' | 'dashboard' | 'user-approvals';

export default function HomePage() {
  const [currentView, setCurrentView] = useState<View>('list');
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [showSettings, setShowSettings] = useState(false);
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
    // Prevent CUSTOMER_AGENT from accessing dashboard or user management
    if (user?.role === 'CUSTOMER_AGENT' && (view === 'dashboard' || view === 'user-approvals')) {
      return;
    }
    setCurrentView(view);
    setSelectedCustomer(null);
  };

  // Redirect CUSTOMER_AGENT users away from restricted views
  useEffect(() => {
    if (user?.role === 'CUSTOMER_AGENT' && (currentView === 'dashboard' || currentView === 'user-approvals')) {
      setCurrentView('list');
    }
  }, [user, currentView]);

  return (
    <AuthGuard>
      <div className="min-h-screen bg-surface-50 flex">
        {/* Left Sidebar */}
        <aside className="w-64 bg-white shadow-md-2 fixed h-full z-50 flex flex-col">
          {/* Logo and Brand */}
          <div className="p-6 border-b border-surface-200">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary-500 rounded-xl flex items-center justify-center shadow-md-2">
                <span className="text-white font-bold text-lg">CT</span>
              </div>
              <div>
                <h1 className="text-headline-5 text-surface-900">{t('app.customerTracker')}</h1>
              </div>
            </div>
          </div>

          {/* Navigation Menu */}
          <nav className="flex-1 p-4 space-y-2">
            <button
              onClick={() => handleNavigation('list')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                currentView === 'list'
                  ? 'bg-primary-100 text-primary-700 border border-primary-200'
                  : 'text-surface-700 hover:bg-surface-100'
              }`}
            >
              <Users size={20} />
              <span className="font-medium">{t('nav.customers')}</span>
            </button>

            {user?.role !== 'CUSTOMER_AGENT' && (
              <button
                onClick={() => handleNavigation('dashboard')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  currentView === 'dashboard'
                    ? 'bg-primary-100 text-primary-700 border border-primary-200'
                    : 'text-surface-700 hover:bg-surface-100'
                }`}
              >
                <BarChart3 size={20} />
                <span className="font-medium">{t('nav.dashboard')}</span>
              </button>
            )}

            {user?.role === 'ADMIN' && (
              <button
                onClick={() => handleNavigation('user-approvals')}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  currentView === 'user-approvals'
                    ? 'bg-primary-100 text-primary-700 border border-primary-200'
                    : 'text-surface-700 hover:bg-surface-100'
                }`}
              >
                <UserCheck size={20} />
                <span className="font-medium">{t('nav.userManagement')}</span>
              </button>
            )}
          </nav>

          {/* User Info Section at Bottom */}
          <div className="p-4 border-t border-surface-200">
            <button
              onClick={() => setShowSettings(true)}
              className="w-full flex items-center gap-3 px-4 py-3 bg-surface-100 rounded-lg hover:bg-surface-200 transition-colors"
            >
              {user?.role === 'ADMIN' ? (
                <Shield size={20} className="text-primary-600" />
              ) : (
                <User size={20} className="text-surface-600" />
              )}
              <div className="flex-1 text-left">
                <div className="text-sm font-medium text-surface-700">
                  {user?.phone}
                </div>
                {user?.role === 'ADMIN' && (
                  <div className="text-xs text-surface-500">{t('app.admin')}</div>
                )}
              </div>
              <Settings size={16} className="text-surface-500" />
            </button>
          </div>
        </aside>

        {/* Main Content Area */}
        <div className="flex-1 ml-64">
          <main className="p-8">
        {currentView === 'list' && (
          <CustomerList 
            key={refreshTrigger}
            onCustomerSelect={handleCustomerSelect}
            onCreateCustomer={handleCreateCustomer}
          />
        )}

        {currentView === 'detail' && selectedCustomer && (
          <CustomerDetail 
            customerId={selectedCustomer.id}
            onBack={handleBack}
          />
        )}

        {currentView === 'create' && (
          <CustomerForm 
            onClose={handleFormClose}
            onSuccess={handleFormSuccess}
          />
        )}

        {currentView === 'dashboard' && user?.role === 'ADMIN' && (
          <AdminDashboard />
        )}

        {currentView === 'dashboard' && user?.role !== 'ADMIN' && (
          <div>
            {/* Import and render Sales Dashboard component without navigation */}
            <SalesDashboardInline onNavigateToCustomers={() => handleNavigation('list')} />
          </div>
        )}

        {currentView === 'user-approvals' && (
          <UserManagementPage />
        )}
      </main>
      
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
