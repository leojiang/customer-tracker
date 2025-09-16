'use client';

import { useState } from 'react';
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
    setCurrentView(view);
    setSelectedCustomer(null);
  };

  return (
    <AuthGuard>
      <div className="min-h-screen bg-surface-50">
        {/* App Bar */}
        <header className="bg-white shadow-md-1 sticky top-0 z-50">
          <div className="container mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-primary-500 rounded-xl flex items-center justify-center shadow-md-2">
                  <span className="text-white font-bold text-lg">CT</span>
                </div>
                <div>
                  <h1 className="text-headline-5 text-surface-900">{t('app.customerTracker')}</h1>
                  <p className="text-body-2 text-surface-600">{t('app.professionalCRM')}</p>
                </div>
              </div>
              
              {/* Navigation and User Info */}
              <div className="flex items-center gap-4">
                <button
                  onClick={() => handleNavigation('list')}
                  className={`flex items-center gap-2 text-sm py-2 px-3 rounded-lg transition-colors ${
                    currentView === 'list' 
                      ? 'bg-primary-100 text-primary-700 border border-primary-200' 
                      : 'btn-outline'
                  }`}
                >
                  <Users size={16} />
                  {t('nav.customers')}
                </button>
                
                <button
                  onClick={() => handleNavigation('dashboard')}
                  className={`flex items-center gap-2 text-sm py-2 px-3 rounded-lg transition-colors ${
                    currentView === 'dashboard' 
                      ? 'bg-primary-100 text-primary-700 border border-primary-200' 
                      : 'btn-outline'
                  }`}
                >
                  <BarChart3 size={16} />
                  {t('nav.dashboard')}
                </button>
                
                {user?.role === 'ADMIN' && (
                  <button
                    onClick={() => handleNavigation('user-approvals')}
                    className={`flex items-center gap-2 text-sm py-2 px-3 rounded-lg transition-colors ${
                      currentView === 'user-approvals' 
                        ? 'bg-primary-100 text-primary-700 border border-primary-200' 
                        : 'btn-outline'
                    }`}
                  >
                    <UserCheck size={16} />
                    {t('nav.userManagement')}
                  </button>
                )}
                
                {/* Settings button - clickable user phone */}
                <button
                  onClick={() => setShowSettings(true)}
                  className="flex items-center gap-2 px-3 py-2 bg-surface-100 rounded-lg hover:bg-surface-200 transition-colors"
                >
                  {user?.role === 'ADMIN' ? (
                    <Shield size={16} className="text-primary-600" />
                  ) : (
                    <User size={16} className="text-surface-600" />
                  )}
                  <span className="text-sm font-medium text-surface-700">
                    {user?.phone} {user?.role === 'ADMIN' && t('app.admin')}
                  </span>
                  <Settings size={14} className="text-surface-500" />
                </button>
              </div>
            </div>
          </div>
        </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-8">
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
