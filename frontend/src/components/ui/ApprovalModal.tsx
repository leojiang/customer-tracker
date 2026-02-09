'use client';

import { useState, useEffect } from 'react';
import { X, CheckCircle, XCircle, Clock } from 'lucide-react';
import { useLanguage } from '@/contexts/LanguageContext';

interface ApprovalModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (reason?: string, salesRole?: string) => void;
  type: 'approve' | 'reject' | 'reset' | 'enable' | 'disable';
  userPhone: string;
  loading?: boolean;
  currentRole?: string; // Add current role prop
}

export default function ApprovalModal({
  isOpen,
  onClose,
  onConfirm,
  type,
  userPhone,
  loading = false,
  currentRole
}: ApprovalModalProps) {
  const { t } = useLanguage();
  const [reason, setReason] = useState('');
  const [selectedRole, setSelectedRole] = useState(currentRole || '');

  // Update selectedRole when currentRole changes and modal opens
  useEffect(() => {
    if (isOpen) {
      setSelectedRole(currentRole || '');
    }
  }, [isOpen, currentRole]);

  if (!isOpen) {
    return null;
  }

  const getModalConfig = () => {
    switch (type) {
      case 'approve':
        return {
          title: t('approvals.approveTitle'),
          description: `${t('approvals.approveDescription')} ${userPhone}?`,
          icon: <CheckCircle className="w-6 h-6 text-green-600" />,
          buttonText: t('approvals.approveButton'),
          buttonClass: 'bg-green-600 hover:bg-green-700 text-white',
          reasonLabel: t('approvals.approveReason'),
          reasonPlaceholder: t('approvals.approvePlaceholder')
        };
      case 'reject':
        return {
          title: t('approvals.rejectTitle'),
          description: `${t('approvals.rejectDescription')} ${userPhone}?`,
          icon: <XCircle className="w-6 h-6 text-red-600" />,
          buttonText: t('approvals.rejectButton'),
          buttonClass: 'bg-red-600 hover:bg-red-700 text-white',
          reasonLabel: t('approvals.rejectReason'),
          reasonPlaceholder: t('approvals.rejectPlaceholder')
        };
      case 'reset':
        return {
          title: t('approvals.resetTitle'),
          description: `${t('approvals.resetDescription')} ${userPhone} ${t('approvals.resetDescription2')}`,
          icon: <Clock className="w-6 h-6 text-orange-600" />,
          buttonText: t('approvals.resetButton'),
          buttonClass: 'bg-orange-600 hover:bg-orange-700 text-white',
          reasonLabel: t('approvals.resetReason'),
          reasonPlaceholder: t('approvals.resetPlaceholder')
        };
      case 'enable':
        return {
          title: t('userManagement.enableTitle'),
          description: `${t('userManagement.enableDescription')} ${userPhone}?`,
          icon: <CheckCircle className="w-6 h-6 text-green-600" />,
          buttonText: t('userManagement.enableButton'),
          buttonClass: 'bg-green-600 hover:bg-green-700 text-white',
          reasonLabel: t('userManagement.enableReason'),
          reasonPlaceholder: t('userManagement.enablePlaceholder')
        };
      case 'disable':
        return {
          title: t('userManagement.disableTitle'),
          description: `${t('userManagement.disableDescription')} ${userPhone}?`,
          icon: <XCircle className="w-6 h-6 text-red-600" />,
          buttonText: t('userManagement.disableButton'),
          buttonClass: 'bg-red-600 hover:bg-red-700 text-white',
          reasonLabel: t('userManagement.disableReason'),
          reasonPlaceholder: t('userManagement.disablePlaceholder')
        };
      default:
        return {
          title: '',
          description: '',
          icon: null,
          buttonText: '',
          buttonClass: '',
          reasonLabel: '',
          reasonPlaceholder: ''
        };
    }
  };

  const config = getModalConfig();

  const handleConfirm = () => {
    if ((type === 'reject' || type === 'disable') && !reason.trim()) {
      return; // Rejection and disable require a reason
    }
    // Only pass salesRole if it's different from currentRole and type is approve
    const salesRoleParam = (type === 'approve' && selectedRole && selectedRole !== currentRole)
      ? selectedRole
      : undefined;
    onConfirm(reason.trim() || undefined, salesRoleParam);
  };

  const handleClose = () => {
    setReason('');
    setSelectedRole(currentRole || '');
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            {config.icon}
            <h3 className="text-lg font-semibold text-gray-900">{config.title}</h3>
          </div>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={loading}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <p className="text-gray-600 mb-4">{config.description}</p>

          <div className="space-y-4">
            {/* Role selector - only show for approve type */}
            {type === 'approve' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t('register.role')}
                </label>
                <select
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  disabled={loading}
                >
                  <option value="">{t('approvals.selectRole')}</option>
                  <option value="ADMIN">{t('role.admin')}</option>
                  <option value="OFFICER">{t('role.officer')}</option>
                  <option value="CUSTOMER_AGENT">{t('role.customerAgent')}</option>
                </select>
                <p className="text-sm text-gray-500 mt-1">{t('approvals.roleChangeHint')}</p>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                {config.reasonLabel}
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none"
                placeholder={config.reasonPlaceholder}
                disabled={loading}
              />
              {(type === 'reject' || type === 'disable') && (
                <p className="text-sm text-red-600 mt-1">{t('approvals.reasonRequired')}</p>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 bg-gray-50 rounded-b-lg">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition-colors"
            disabled={loading}
          >
            {t('customers.form.cancel')}
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading || ((type === 'reject' || type === 'disable') && !reason.trim())}
            className={`px-4 py-2 text-sm font-medium rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${config.buttonClass}`}
          >
            {loading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                {t('approvals.processing')}
              </div>
            ) : (
              config.buttonText
            )}
          </button>
        </div>
      </div>
    </div>
  );
}