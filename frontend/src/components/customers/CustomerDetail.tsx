'use client';

import { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Edit, Phone, Building2, MapPin, User, GraduationCap, Briefcase, Save, X, AlertCircle, Trash2, UserCircle, Calendar, IdCard } from 'lucide-react';
import { Customer, CustomerStatus, CustomerType, StatusTransitionRequest, UpdateCustomerRequest, EducationLevel, EducationLevelDisplayNames, getTranslatedStatusName, getTranslatedEducationLevelName, getTranslatedCustomerTypeName, CertificateType, CertificateTypeTranslationKeys, CertificateIssuer, CertificateIssuerTranslationKeys, CustomerTypeTranslationKeys } from '@/types/customer';
import { customerApi, customerDeleteRequestApi } from '@/lib/api';
import { getErrorMessage } from '@/lib/errorHandler';
import { mapCertificateIssuerToDisplay, getCertificateIssuerOptions } from '@/lib/certificateIssuerUtils';
import { getCertificateTypeDisplayName } from '@/lib/certificateTypeUtils';
import StatusBadge from '@/components/ui/StatusBadge';
import StatusHistory from '@/components/customers/StatusHistory';
import { validatePhoneNumber, validateName, validateAge, formatPhoneNumber } from '@/lib/validation';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { useUserManagementRefresh } from '@/contexts/UserManagementRefreshContext';
import DeleteRequestModal from '@/components/ui/DeleteRequestModal';
import AlertModal from '@/components/ui/AlertModal';
import { SalesRole } from '@/types/auth';
// import { format } from 'date-fns'; // Unused import removed

interface CustomerDetailProps {
  customerId: string;
  onBack: () => void;
}

export default function CustomerDetail({ customerId, onBack }: CustomerDetailProps) {
  const { t } = useLanguage();
  const { user, token } = useAuth();

  // Try to use the refresh context, but handle cases where it's not available
  let refreshDeleteRequests: (() => void) | null = null;
  try {
    const refreshContext = useUserManagementRefresh();
    refreshDeleteRequests = refreshContext.refreshDeleteRequests;
  } catch (error) {
    // Context not available - this is expected for non-admin pages
    refreshDeleteRequests = null;
  }
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<UpdateCustomerRequest>({
    name: '',
    phone: '',
    certificateIssuer: '',
    businessRequirements: '',
    certificateType: undefined,
    age: undefined,
    education: undefined,
    gender: '',
    address: '',
    idCard: undefined,
    customerAgent: '',
    customerType: CustomerType.NEW_CUSTOMER,
    certifiedAt: undefined,
  });
  const [statusTransition, setStatusTransition] = useState<StatusTransitionRequest>({
    toStatus: CustomerStatus.NEW,
    reason: '',
  });
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [historyRefreshTrigger, setHistoryRefreshTrigger] = useState(0);
  const [validTransitions, setValidTransitions] = useState<CustomerStatus[]>([]);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [idCardError, setIdCardError] = useState<string>('');
  const [showDeleteRequestModal, setShowDeleteRequestModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [alertModal, setAlertModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    type?: 'error' | 'warning' | 'info';
  }>({
    isOpen: false,
    title: '',
    message: '',
  });
  const [toast, setToast] = useState<{
    show: boolean;
    message: string;
    type?: 'success' | 'error';
  }>({
    show: false,
    message: '',
  });

  const loadValidTransitions = useCallback(async () => {
    try {
      const transitions = await customerApi.getValidTransitions(customerId);
      setValidTransitions(transitions);
    } catch (err) {
      // Failed to load valid transitions, fallback to empty array
      // Status transition UI will be disabled
      setValidTransitions([]);
    }
  }, [customerId]);

  const loadCustomer = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await customerApi.getCustomer(customerId);
      setCustomer(data);
      setEditForm({
        name: data.name,
        phone: data.phone,
        certificateIssuer: mapCertificateIssuerToDisplay(data.certificateIssuer),
        businessRequirements: data.businessRequirements || '',
        certificateType: data.certificateType,
        age: data.age,
        education: data.education || undefined,
        gender: data.gender || '',
        address: data.address || '',
        idCard: data.idCard,
        customerAgent: data.customerAgent || '',
        customerType: data.customerType || CustomerType.NEW_CUSTOMER,
        certifiedAt: data.certifiedAt,
      });
      // Load valid transitions after customer is loaded
      await loadValidTransitions();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('customers.detail.failedLoad'));
    } finally {
      setLoading(false);
    }
  }, [customerId, loadValidTransitions, t]);

  useEffect(() => {
    loadCustomer();
  }, [loadCustomer]);

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    if (customer) {
      setEditForm({
        name: customer.name,
        phone: customer.phone,
        certificateIssuer: customer.certificateIssuer || '',
        businessRequirements: customer.businessRequirements || '',
        certificateType: customer.certificateType,
        age: customer.age,
        education: customer.education || undefined,
        gender: customer.gender || '',
        address: customer.address || '',
        idCard: customer.idCard,
        customerAgent: customer.customerAgent || '',
        customerType: customer.customerType || CustomerType.NEW_CUSTOMER,
        certifiedAt: customer.certifiedAt,
      });
    }
    setIsEditing(false);
    setFieldErrors({});
  };

  const handleEditFormChange = (field: keyof UpdateCustomerRequest, value: string | number | undefined) => {
    setEditForm(prev => ({ ...prev, [field]: value }));

    // Clear field-specific errors
    if (fieldErrors[field]) {
      setFieldErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validateEditField = (field: keyof UpdateCustomerRequest, value: string | number | undefined) => {
    let validation;

    switch (field) {
      case 'name':
        validation = validateName(value as string);
        break;
      case 'phone':
        validation = validatePhoneNumber(value as string);
        break;
      case 'age':
        validation = validateAge(value as number);
        break;
      default:
        return;
    }

    if (!validation.isValid) {
      setFieldErrors(prev => ({ ...prev, [field]: validation.message! }));
    } else {
      setFieldErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const handleSave = async () => {
    if (!customer) {
      return;
    }

    // Clear previous errors and toasts
    setError(null);
    setFieldErrors({});
    setAlertModal({ isOpen: false, title: '', message: '' });

    // Validate required fields
    const nameValidation = validateName(editForm.name);
    const phoneValidation = validatePhoneNumber(editForm.phone);
    const ageValidation = validateAge(editForm.age);

    const newFieldErrors: Record<string, string> = {};

    if (!nameValidation.isValid) {
      newFieldErrors.name = nameValidation.message!;
    }

    if (!phoneValidation.isValid) {
      newFieldErrors.phone = phoneValidation.message!;
    }

    if (!ageValidation.isValid) {
      newFieldErrors.age = ageValidation.message!;
    }

    // Validate certificate type is required
    if (!editForm.certificateType) {
      newFieldErrors.certificateType = t('validation.certificateTypeRequired');
    }

    if (Object.keys(newFieldErrors).length > 0) {
      setFieldErrors(newFieldErrors);
      return;
    }

    try {
      setUpdating(true);

      // Format phone number before sending
      const cleanedForm = {
        ...editForm,
        phone: formatPhoneNumber(editForm.phone)
      };

      const updatedCustomer = await customerApi.updateCustomer(customer.id, cleanedForm);
      setCustomer(updatedCustomer);
      setIsEditing(false);

      // Show success toast
      setToast({
        show: true,
        message: t('customers.updateSuccess'),
        type: 'success'
      });

      // Auto-hide toast after 1 second
      setTimeout(() => {
        setToast({ show: false, message: '' });
      }, 1000);
    } catch (err) {
      // Use the error handler to map business error codes to user-friendly messages
      const userErrorMessage = getErrorMessage(err, t);

      // Show error in modal instead of inline
      setAlertModal({
        isOpen: true,
        title: t('error.updateFailed'),
        message: userErrorMessage,
        type: 'error'
      });
    } finally {
      setUpdating(false);
    }
  };

  const handleStatusTransition = async () => {
    if (!customer) {
      return;
    }

    try {
      setUpdating(true);
      const updatedCustomer = await customerApi.transitionStatus(customer.id, statusTransition);
      // Status updated successfully

      // Update customer state immediately
      setCustomer(updatedCustomer);

      // Also re-fetch to ensure we have the latest data
      await loadCustomer();

      setShowStatusModal(false);
      setStatusTransition({ toStatus: CustomerStatus.NEW, reason: '' });
      // Trigger history refresh
      setHistoryRefreshTrigger(prev => prev + 1);
    } catch (err) {
      // Show error in modal popup
      const userErrorMessage = getErrorMessage(err, t);
      setAlertModal({
        isOpen: true,
        title: t('customers.detail.statusTransitionFailed'),
        message: userErrorMessage,
        type: 'error'
      });
    } finally {
      setUpdating(false);
    }
  };

  const getAvailableStatusTransitions = (): CustomerStatus[] => {
    // Return valid transitions from backend validation
    return validTransitions;
  };

  const handleDeleteRequest = async (reason: string) => {
    if (!customer || !token) {
      return;
    }

    try {
      setIsDeleting(true);
      await customerDeleteRequestApi.createDeleteRequest(token, {
        customerId: customer.id,
        reason
      });
      // Refresh admin delete requests list (if available)
      if (refreshDeleteRequests) {
        await refreshDeleteRequests();
      }
      // Success is handled by the modal, just close it
      setShowDeleteRequestModal(false);
      setIsDeleting(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      setIsDeleting(false);

      // Check if it's a duplicate request error
      if (errorMessage.includes('pending delete request already exists')) {
        // Close the delete request modal first
        setShowDeleteRequestModal(false);
        // Show alert modal with the duplicate request message
        setAlertModal({
          isOpen: true,
          title: t('deleteRequests.requestDeletion'),
          message: t('deleteRequests.pendingRequestExists'),
          type: 'warning',
        });
      } else {
        // Re-throw the error so the DeleteRequestModal can display it
        throw err;
      }
    }
  };

  const canEdit = (): boolean => {
    if (!user) {
      return false;
    }
    return user.role === SalesRole.ADMIN;
  };

  const canRequestDelete = (): boolean => {
    if (!user) {
      return false;
    }
    return user.role === SalesRole.ADMIN;
  };

  const getSalesPersonDisplayName = (salesPhone: string): string => {
    if (!salesPhone) {
      return t('customers.form.salesPerson.unknown');
    }

    // If it's the current user, show "You"
    if (user && salesPhone === user.phone) {
      return t('customers.form.salesPerson.you');
    }

    // Otherwise show phone number
    return salesPhone;
  };

  const getLocalizedGender = (gender: string | undefined): string => {
    if (!gender) {
      return '';
    }
    switch (gender.toLowerCase()) {
      case 'male':
        return t('customers.form.gender.male');
      case 'female':
        return t('customers.form.gender.female');
      case 'other':
        return t('customers.form.gender.other');
      default:
        return gender;
    }
  };

  const availableTransitions = getAvailableStatusTransitions();

  if (loading) {
    return (
      <div className="card p-6 text-center">
        <div className="text-gray-600">{t('customers.detail.loading')}</div>
      </div>
    );
  }

  if (error || !customer) {
    return (
      <div className="card p-6 text-center">
        <div className="text-red-600 mb-4">{t('customers.detail.failedLoad')}</div>
        <p className="text-gray-600 mb-4">{error || t('customers.detail.notFound')}</p>
        <div className="flex gap-2 justify-center">
          <button onClick={onBack} className="btn-secondary">
            {t('customers.detail.back')}
          </button>
          <button onClick={loadCustomer} className="btn-primary">
            {t('customers.tryAgain')}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="px-8 py-6 space-y-8">
      {/* Header */}
      <div className="flex items-center">
        <button onClick={onBack} className="btn-secondary flex items-center gap-3">
          <ArrowLeft size={20} />
          {t('customers.detail.back')}
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-stretch">
        <div className="lg:col-span-2 flex">
          <div className="card-elevated flex-1 flex flex-col" id="customer-info-card">
            <div className="card-header flex-shrink-0">
              <div className="flex justify-between items-center">
                <h2 className="text-headline-5">{t('customers.detail.customerDetails')}</h2>
                <div className="flex gap-3">
                  {isEditing ? (
                    <>
                      <button
                        onClick={handleSave}
                        disabled={updating}
                        className="btn-primary flex items-center justify-center gap-2 text-sm px-4 py-2"
                      >
                        <Save size={16} />
                        {updating ? t('customers.form.updating') : t('customers.form.update')}
                      </button>
                      <button
                        onClick={handleCancelEdit}
                        className="btn-secondary flex items-center justify-center gap-2 text-sm px-4 py-2"
                      >
                        <X size={16} />
                        {t('customers.form.cancel')}
                      </button>
                    </>
                  ) : (
                    <>
                      {canEdit() && (
                        <button onClick={handleEdit} className="btn-outline flex items-center justify-center gap-2 text-sm px-4 py-2">
                          <Edit size={16} />
                          {t('customers.detail.edit')}
                        </button>
                      )}
                      {canRequestDelete() && (
                        <button
                          onClick={() => setShowDeleteRequestModal(true)}
                          disabled={isDeleting}
                          className="btn-outline flex items-center justify-center gap-2 text-sm px-4 py-2 text-orange-600 hover:text-orange-700 hover:bg-orange-50"
                        >
                          <Trash2 size={16} />
                          {t('deleteRequests.requestDeletion')}
                        </button>
                      )}
                    </>
                  )}
                </div>
              </div>
            </div>
            <div className="card-content flex-grow">
              <div className="mb-8">
                {isEditing ? (
                  <div>
                    <label className='input-label'>{t('customers.form.name')}</label>
                    <input
                      type="text"
                      value={editForm.name}
                      onChange={(e) => handleEditFormChange('name', e.target.value)}
                      onBlur={(e) => validateEditField('name', e.target.value)}
                      className={`input-field text-headline-4 font-medium ${fieldErrors.name ? 'border-red-500' : ''}`}
                    />
                    {fieldErrors.name && (
                      <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                        <AlertCircle size={14} />
                        <span>{fieldErrors.name}</span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="flex items-center gap-3">
                    <h2 className="text-headline-3">{customer.name}</h2>
                    <StatusBadge key={customer.currentStatus} status={customer.currentStatus} />
                  </div>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="space-y-6">
                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Phone size={18} className="text-surface-500" />
                      {t('customers.form.phone')}
                    </label>
                    {isEditing ? (
                      <div>
                        <input
                          type="tel"
                          value={editForm.phone}
                          onChange={(e) => handleEditFormChange('phone', e.target.value)}
                          onBlur={(e) => validateEditField('phone', e.target.value)}
                          className={`input-field ${fieldErrors.phone ? 'border-red-500' : ''}`}
                          placeholder={t('customers.form.phone')}
                        />
                        {fieldErrors.phone && (
                          <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                            <AlertCircle size={14} />
                            <span>{fieldErrors.phone}</span>
                          </div>
                        )}
                      </div>
                    ) : (
                      <p className="text-body-1">{customer.phone}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <IdCard size={18} className="text-surface-500" />
                      {t('customers.form.idCard')}
                    </label>
                    {isEditing ? (
                      <>
                        <input
                          type="text"
                          value={editForm.idCard || ''}
                          onChange={(e) => {
                            const value = e.target.value;
                            // Check if value contains only digits and English letters
                            if (value === '' || /^[a-zA-Z0-9]*$/.test(value)) {
                              setEditForm(prev => ({ ...prev, idCard: value || undefined }));
                              setIdCardError('');
                            } else {
                              setIdCardError(t('customers.form.idCardHelp'));
                            }
                          }}
                          className={`input-field ${idCardError ? 'border-red-500' : ''}`}
                          placeholder={t('customers.form.idCardPlaceholder')}
                        />
                        {idCardError && (
                          <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                            <AlertCircle size={14} />
                            <span>{idCardError}</span>
                          </div>
                        )}
                      </>
                    ) : (
                      <p className="text-body-1">
                        {customer.idCard
                          ? customer.idCard
                          : <span className="text-surface-400 italic">{t('customers.form.noIdCard')}</span>
                        }
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <User size={18} className="text-surface-500" />
                      {t('customers.form.customerAgent')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.customerAgent}
                        onChange={(e) => setEditForm(prev => ({ ...prev, customerAgent: e.target.value }))}
                        className="input-field"
                        placeholder={t('customers.form.customerAgent.placeholder')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.customerAgent || <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Briefcase size={18} className="text-surface-500" />
                      {t('customers.form.customerType')}
                    </label>
                    {isEditing ? (
                      <select
                        value={editForm.customerType || ''}
                        onChange={(e) => {
                          const value = e.target.value;
                          const customerTypeValue = value && value !== '' ? value as CustomerType : CustomerType.NEW_CUSTOMER;
                          setEditForm(prev => ({ ...prev, customerType: customerTypeValue }));
                        }}
                        className="input-field"
                      >
                        {Object.entries(CustomerTypeTranslationKeys).map(([key]) => (
                          <option key={key} value={key}>
                            {t(CustomerTypeTranslationKeys[key as CustomerType])}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <p className="text-body-1">
                        {customer.customerType ? getTranslatedCustomerTypeName(customer.customerType, t) : <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <UserCircle size={18} className="text-surface-500" />
                      {t('customers.form.salesPerson')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={getSalesPersonDisplayName(customer.salesPhone || '')}
                        className="input-field"
                        readOnly
                      />
                    ) : (
                      <p className="text-body-1">{getSalesPersonDisplayName(customer.salesPhone || '')}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Building2 size={18} className="text-surface-500" />
                      {t('customers.form.certificateIssuer')}
                    </label>
                    {isEditing ? (
                      <select
                        value={editForm.certificateIssuer || ''}
                        onChange={(e) => {
                          const value = e.target.value;
                          const certificateIssuerValue = value && value !== '' ? value as CertificateIssuer : CertificateIssuer.OTHER;
                          setEditForm(prev => ({ ...prev, certificateIssuer: certificateIssuerValue }));
                        }}
                        className="input-field"
                      >
                        {getCertificateIssuerOptions().map((option) => (
                          <option key={option.value} value={option.value}>
                            {t(CertificateIssuerTranslationKeys[option.value])}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <p className="text-body-1">
                  {customer.certificateIssuer ? (
                    t(CertificateIssuerTranslationKeys[mapCertificateIssuerToDisplay(customer.certificateIssuer)])
                  ) : (
                    <span className="text-surface-400 italic">{t('customers.form.certificateIssuer')}</span>
                  )}
                </p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Calendar size={18} className="text-surface-500" />
                      {t('customers.form.certifiedAt')}
                    </label>
                    {isEditing ? (
                      <input
                        type="date"
                        value={editForm.certifiedAt || ''}
                        onChange={(e) => setEditForm(prev => ({ ...prev, certifiedAt: e.target.value || undefined }))}
                        className="input-field"
                        placeholder={t('customers.form.certifiedAt.placeholder')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.certifiedAt || <span className="text-surface-400 italic">{t('customers.form.certifiedAt.placeholder')}</span>}</p>
                    )}
                  </div>
                </div>

                <div className="space-y-6">
                  <div>
                    <label className="input-label flex items-center gap-2">
                      <User size={18} className="text-surface-500" />
                      {t('customers.form.age')}
                    </label>
                    {isEditing ? (
                      <div>
                        <input
                          type="number"
                          value={editForm.age || ''}
                          onChange={(e) => handleEditFormChange('age', e.target.value ? parseInt(e.target.value) : undefined)}
                          onBlur={(e) => validateEditField('age', e.target.value ? parseInt(e.target.value) : undefined)}
                          className={`input-field ${fieldErrors.age ? 'border-red-500' : ''}`}
                          placeholder={t('customers.form.age')}
                          min="1"
                          max="120"
                        />
                        {fieldErrors.age && (
                          <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                            <AlertCircle size={14} />
                            <span>{fieldErrors.age}</span>
                          </div>
                        )}
                      </div>
                    ) : (
                      <p className="text-body-1">{customer.age || <span className="text-surface-400 italic">{t('customers.form.age')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <GraduationCap size={18} className="text-surface-500" />
                      {t('customers.form.education')}
                    </label>
                    {isEditing ? (
                      <select
                        value={editForm.education || ''}
                        onChange={(e) => handleEditFormChange('education', e.target.value && e.target.value !== '' ? e.target.value as EducationLevel : undefined)}
                        className="input-field"
                      >
                        <option value="">{t('customers.detail.selectEducationLevel')}</option>
                        {Object.entries(EducationLevelDisplayNames).map(([key]) => (
                          <option key={key} value={key}>
                            {getTranslatedEducationLevelName(key as EducationLevel, t)}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <p className="text-body-1">
                        {customer.education ? getTranslatedEducationLevelName(customer.education, t) : <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>}
                      </p>
                    )}
                  <div className="h-1"></div>
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <User size={18} className="text-surface-500" />
                      {t('customers.form.gender')}
                    </label>
                    {isEditing ? (
                      <select
                        value={editForm.gender}
                        onChange={(e) => setEditForm(prev => ({ ...prev, gender: e.target.value }))}
                        className="input-field"
                      >
                        <option value="">{t('customers.form.gender.select')}</option>
                        <option value="male">{t('customers.form.gender.male')}</option>
                        <option value="female">{t('customers.form.gender.female')}</option>
                        <option value="other">{t('customers.form.gender.other')}</option>
                      </select>
                    ) : (
                      <p className="text-body-1">{getLocalizedGender(customer.gender) || <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>}</p>
                    )}
                  <div className="h-1"></div>
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <MapPin size={18} className="text-surface-500" />
                      {t('customers.form.address')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.address}
                        onChange={(e) => handleEditFormChange('address', e.target.value)}
                        className="input-field"
                        placeholder={t('customers.form.address')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.address || <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Briefcase size={18} className="text-surface-500" />
                      {t('customers.form.certificateType')} *
                    </label>
                    {isEditing ? (
                      <>
                        <select
                          value={editForm.certificateType || ''}
                          onChange={(e) => {
                            const value = e.target.value;
                            const certificateValue = value && value !== '' ? value as CertificateType : undefined;
                            setEditForm(prev => ({ ...prev, certificateType: certificateValue }));
                            // Clear field error when user makes a selection
                            if (certificateValue && fieldErrors.certificateType) {
                              setFieldErrors(prev => {
                                const newErrors = { ...prev };
                                delete newErrors.certificateType;
                                return newErrors;
                              });
                            }
                          }}
                          className={`input-field ${fieldErrors.certificateType ? 'border-red-500' : ''}`}
                          required
                        >
                          <option value="">{t('customers.form.selectCertificateType')}</option>
                          {Object.entries(CertificateTypeTranslationKeys).map(([key]) => (
                            <option key={key} value={key}>
                              {t(CertificateTypeTranslationKeys[key as CertificateType])}
                            </option>
                          ))}
                        </select>
                        {fieldErrors.certificateType && (
                          <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                            <AlertCircle size={14} />
                            <span>{fieldErrors.certificateType}</span>
                          </div>
                        )}
                      </>
                    ) : (
                      <p className="text-body-1">
                        {customer.certificateType ? (
                          getCertificateTypeDisplayName(customer.certificateType as CertificateType, t)
                        ) : (
                          <span className="text-surface-400 italic">{t('customers.detail.notSpecified')}</span>
                        )}
                      </p>
                    )}
                    <div className="h-1"></div>
                  </div>

                  {/* Business Requirements */}
                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Briefcase size={18} className="text-surface-500" />
                      {t('customers.form.businessReq')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.businessRequirements}
                        onChange={(e) => setEditForm(prev => ({ ...prev, businessRequirements: e.target.value }))}
                        className="input-field"
                        placeholder={t('customers.form.businessReq')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.businessRequirements || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col h-full space-y-6">
          {/* Status Management Card - Hidden for OFFICER */}
          {user?.role !== SalesRole.OFFICER && (
            <div className="card-elevated flex-shrink-0">
              <div className="card-header">
                <h3 className="text-headline-6">{t('customers.detail.statusManagement')}</h3>
              </div>
              <div className="card-content">
                {availableTransitions.length > 0 ? (
                  <button
                    onClick={() => {
                      if (availableTransitions.length > 0) {
                        setStatusTransition({
                          toStatus: availableTransitions[0]!,
                          reason: ''
                        });
                        setShowStatusModal(true);
                      }
                    }}
                    className="btn-primary w-full flex items-center justify-center gap-3"
                  >
                    <ArrowLeft className="rotate-90" size={18} />
                    {t('customers.detail.updateStatus')}
                  </button>
                ) : (
                  <div className="text-center py-6">
                    <div className="text-body-2 text-surface-500 mb-2">{t('customers.detail.noTransitionsAvailable')}</div>
                    <p className="text-caption text-surface-400">
                      {customer.currentStatus === 'CERTIFIED'
                        ? t('customers.detail.businessComplete')
                        : t('customers.detail.loadingTransitions')}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Status History Component */}
          <div className="flex-grow flex flex-col">
            <StatusHistory customerId={customer.id} refreshTrigger={historyRefreshTrigger} />
          </div>
        </div>
      </div>

      {showStatusModal && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-md-5 transform transition-all">
            <div className="card-header rounded-t-2xl">
              <h3 className="text-headline-5 flex items-center gap-3">
                <ArrowLeft className="rotate-90 text-primary-500" size={20} />
                {t('customers.detail.updateStatus')}
              </h3>
            </div>

            <div className="card-content space-y-6">
              <div>
                <label className="input-label">
                  {t('customers.detail.newStatus')}
                </label>
                <select
                  value={statusTransition.toStatus}
                  onChange={(e) => setStatusTransition(prev => ({ ...prev, toStatus: e.target.value as CustomerStatus }))}
                  className="input-field focus-ring"
                >
                  {availableTransitions.map(status => (
                    <option key={status} value={status}>
                      {getTranslatedStatusName(status, t)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="input-label">
                  {t('customers.detail.reasonOptional')}
                </label>
                <textarea
                  value={statusTransition.reason}
                  onChange={(e) => setStatusTransition(prev => ({ ...prev, reason: e.target.value }))}
                  rows={3}
                  className="input-field focus-ring"
                  placeholder={t('customers.detail.statusReason')}
                />
              </div>
            </div>

            <div className="card-content pt-0">
              <div className="divider mb-6"></div>
              <div className="flex flex-col sm:flex-row gap-3">
                <button
                  onClick={handleStatusTransition}
                  disabled={updating}
                  className="btn-primary flex items-center justify-center gap-3 flex-1"
                >
                  <ArrowLeft className="rotate-90" size={18} />
                  {updating ? t('customers.detail.updating') : t('customers.detail.updateStatus')}
                </button>
                <button
                  onClick={() => setShowStatusModal(false)}
                  className="btn-secondary flex items-center justify-center gap-3 flex-1"
                >
                  {t('customers.form.cancel')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Request Modal for Officers */}
      {customer && (
        <>
          <DeleteRequestModal
            isOpen={showDeleteRequestModal}
            onClose={() => setShowDeleteRequestModal(false)}
            onSubmit={handleDeleteRequest}
            customerName={customer.name}
          />

          {/* Toast Notification */}
          {toast.show && (
            <div
              className={`fixed top-4 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-lg shadow-lg flex items-center gap-2 ${
                toast.type === 'success'
                  ? 'bg-green-500 text-white'
                  : 'bg-red-500 text-white'
              }`}
            >
              {toast.type === 'success' && <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>}
              <span className="font-medium">{toast.message}</span>
            </div>
          )}

          <AlertModal
            isOpen={alertModal.isOpen}
            onClose={() => setAlertModal({ ...alertModal, isOpen: false })}
            title={alertModal.title}
            message={alertModal.message}
            type={alertModal.type}
          />
        </>
      )}
    </div>
  );
}
