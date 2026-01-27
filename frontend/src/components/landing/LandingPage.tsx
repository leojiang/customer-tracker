'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { Users, BarChart3, Upload, UserCheck, ArrowRight } from 'lucide-react';
import { getTranslatedRoleName } from '@/types/auth';

interface LandingPageProps {
  onNavigateToCustomers: () => void;
  onNavigateToDashboard: () => void;
  onNavigateToBatchImport: () => void;
  onNavigateToUserManagement: () => void;
}

export default function LandingPage({
  onNavigateToCustomers,
  onNavigateToDashboard,
  onNavigateToBatchImport,
  onNavigateToUserManagement,
}: LandingPageProps) {
  const { user } = useAuth();
  const { t } = useLanguage();

  const quickActions = [
    {
      title: t('landing.manageCustomers'),
      description: t('landing.manageCustomersDesc'),
      icon: Users,
      action: onNavigateToCustomers,
      color: 'bg-blue-500',
      available: true,
    },
    {
      title: t('nav.dashboard'),
      description: t('landing.viewDashboardDesc'),
      icon: BarChart3,
      action: onNavigateToDashboard,
      color: 'bg-green-500',
      available: user?.role === 'ADMIN',
    },
    {
      title: t('nav.batchImport'),
      description: t('landing.batchImportDesc'),
      icon: Upload,
      action: onNavigateToBatchImport,
      color: 'bg-purple-500',
      available: user?.role === 'ADMIN' || user?.role === 'OFFICER',
    },
    {
      title: t('nav.userManagement'),
      description: t('landing.userManagementDesc'),
      icon: UserCheck,
      action: onNavigateToUserManagement,
      color: 'bg-orange-500',
      available: user?.role === 'ADMIN',
    },
  ];

  const availableActions = quickActions.filter((action) => action.available);

  return (
    <div className="h-full overflow-y-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            {t('landing.welcome')}, {user?.name || user?.phone}
          </h1>
          <p className="text-gray-600 text-lg">
            {t('landing.welcomeMessage')}
          </p>
        </div>

        {/* Quick Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-blue-100 text-sm font-medium mb-1">
                  {t('landing.quickAccess')}
                </p>
                <h3 className="text-2xl font-bold">{availableActions.length}</h3>
                <p className="text-blue-100 text-sm">{t('landing.availableActions')}</p>
              </div>
              <Users size={48} className="text-blue-200 opacity-50" />
            </div>
          </div>

          <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-green-100 text-sm font-medium mb-1">
                  {t('landing.yourRole')}
                </p>
                <h3 className="text-2xl font-bold">
                  {user?.role ? getTranslatedRoleName(user.role, t) : '-'}
                </h3>
                <p className="text-green-100 text-sm">{t('landing.accountStatus')}</p>
              </div>
              <BarChart3 size={48} className="text-green-200 opacity-50" />
            </div>
          </div>

          <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-purple-100 text-sm font-medium mb-1">
                  {t('landing.systemStatus')}
                </p>
                <h3 className="text-2xl font-bold">{t('landing.online')}</h3>
                <p className="text-purple-100 text-sm">{t('landing.ready')}</p>
              </div>
              <Upload size={48} className="text-purple-200 opacity-50" />
            </div>
          </div>
        </div>

        {/* Quick Actions Grid */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            {t('landing.quickActions')}
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {availableActions.map((action) => (
              <button
                key={action.title}
                onClick={action.action}
                className="bg-white rounded-xl p-6 shadow-sm hover:shadow-md transition-all duration-200 text-left border border-gray-200 hover:border-gray-300 group"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className={`${action.color} rounded-lg p-3`}>
                    <action.icon size={24} className="text-white" />
                  </div>
                  <ArrowRight
                    size={20}
                    className="text-gray-400 group-hover:text-gray-600 group-hover:translate-x-1 transition-all"
                  />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  {action.title}
                </h3>
                <p className="text-gray-600 text-sm">{action.description}</p>
              </button>
            ))}
          </div>
        </div>

        {/* Getting Started Section */}
        <div className="mt-8 bg-gradient-to-r from-indigo-50 to-blue-50 rounded-xl p-6 border border-indigo-100">
          <h3 className="text-lg font-semibold text-gray-900 mb-3">
            {t('landing.gettingStarted')}
          </h3>
          <ul className="space-y-2 text-gray-700">
            <li className="flex items-start">
              <span className="text-indigo-600 mr-2">•</span>
              <span>{t('landing.tip1')}</span>
            </li>
            <li className="flex items-start">
              <span className="text-indigo-600 mr-2">•</span>
              <span>{t('landing.tip2')}</span>
            </li>
            <li className="flex items-start">
              <span className="text-indigo-600 mr-2">•</span>
              <span>{t('landing.tip3')}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
