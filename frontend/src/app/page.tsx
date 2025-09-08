'use client';

import { useState } from 'react';
import { Customer } from '@/types/customer';
import CustomerList from '@/components/customers/CustomerList';
import CustomerDetail from '@/components/customers/CustomerDetail';
import CustomerForm from '@/components/customers/CustomerForm';

import AuthGuard from '@/components/auth/AuthGuard';
import { useAuth } from '@/contexts/AuthContext';
import { LogOut, User, Shield, BarChart3, UserCheck } from 'lucide-react';
import { useRouter } from 'next/navigation';

type View = 'list' | 'detail' | 'create';

export default function HomePage() {
  const [currentView, setCurrentView] = useState<View>('list');
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const { user, logout } = useAuth();
  const router = useRouter();

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
                  <h1 className="text-headline-5 text-surface-900">Customer Tracker</h1>
                  <p className="text-body-2 text-surface-600">Professional CRM Solution</p>
                </div>
              </div>
              
              {/* Navigation and User Info */}
              <div className="flex items-center gap-4">
                <button
                  onClick={() => router.push('/dashboard')}
                  className="btn-outline flex items-center gap-2 text-sm py-2 px-3"
                >
                  <BarChart3 size={16} />
                  Dashboard
                </button>
                
                {/* User Approvals button - only show for admin users */}
                {user?.role === 'ADMIN' && (
                  <button
                    onClick={() => router.push('/dashboard/admin/user-approvals')}
                    className="btn-outline flex items-center gap-2 text-sm py-2 px-3 bg-green-50 text-green-700 border-green-300 hover:bg-green-100 hover:border-green-400"
                  >
                    <UserCheck size={16} />
                    User Approvals
                  </button>
                )}
                <div className="flex items-center gap-2 px-3 py-2 bg-surface-100 rounded-lg">
                  {user?.role === 'ADMIN' ? (
                    <Shield size={16} className="text-primary-600" />
                  ) : (
                    <User size={16} className="text-surface-600" />
                  )}
                  <span className="text-sm font-medium text-surface-700">
                    {user?.phone} {user?.role === 'ADMIN' && '(Admin)'}
                  </span>
                </div>
                <button
                  onClick={logout}
                  className="btn-outline flex items-center gap-2 text-sm py-2 px-3"
                >
                  <LogOut size={16} />
                  Logout
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
      </main>
      </div>
    </AuthGuard>
  );
}
