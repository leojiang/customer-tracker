'use client';

import { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Edit, Phone, Building2, MapPin, User, GraduationCap, Briefcase, Save, X, DollarSign, AlertCircle, Trash2, UserCircle, Calendar } from 'lucide-react';
import { Customer, CustomerStatus, StatusTransitionRequest, UpdateCustomerRequest, EducationLevel, EducationLevelDisplayNames, getTranslatedStatusName, getTranslatedEducationLevelName } from '@/types/customer';
import { customerApi, customerDeleteRequestApi } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import StatusHistory from '@/components/customers/StatusHistory';
import { validatePhoneNumber, validateName, validateAge, formatPhoneNumber } from '@/lib/validation';
import GaodeMapPicker, { LocationData } from '@/components/ui/GaodeMapPicker';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
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
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<UpdateCustomerRequest>({
    name: '',
    phone: '',
    company: '',
    businessRequirements: '',
    businessType: '',
    age: undefined,
    education: undefined,
    gender: '',
    location: '',
    price: undefined,
    certifiedAt: undefined,
  });
  const [statusTransition, setStatusTransition] = useState<StatusTransitionRequest>({
    toStatus: CustomerStatus.CUSTOMER_CALLED,
    reason: '',
  });
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [historyRefreshTrigger, setHistoryRefreshTrigger] = useState(0);
  const [validTransitions, setValidTransitions] = useState<CustomerStatus[]>([]);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [showMapPicker, setShowMapPicker] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(null);
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
        company: data.company || '',
        businessRequirements: data.businessRequirements || '',
        businessType: data.businessType || '',
        age: data.age,
        education: data.education || undefined,
        gender: data.gender || '',
        location: data.location || '',
        price: data.price,
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
        company: customer.company || '',
        businessRequirements: customer.businessRequirements || '',
        businessType: customer.businessType || '',
        age: customer.age,
        education: customer.education || undefined,
        gender: customer.gender || '',
        location: customer.location || '',
        price: customer.price,
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

  const handleLocationSelect = (location: LocationData) => {
    setSelectedLocation(location);
    setEditForm(prev => ({ ...prev, location: location.address }));
  };

  const handleOpenMapPicker = () => {
    setShowMapPicker(true);
  };

  const handleCloseMapPicker = () => {
    setShowMapPicker(false);
  };

  const handleSave = async () => {
    if (!customer) {
      return;
    }
    
    // Clear previous errors
    setError(null);
    setFieldErrors({});
    
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
    } catch (err) {
      setError(err instanceof Error ? err.message : t('customers.form.failedUpdate'));
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
      setStatusTransition({ toStatus: CustomerStatus.CUSTOMER_CALLED, reason: '' });
      // Trigger history refresh
      setHistoryRefreshTrigger(prev => prev + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : t('customers.detail.failedLoad'));
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
      // Success is handled by the modal, just close it
      setShowDeleteRequestModal(false);
      setIsDeleting(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      // Check if it's a duplicate request error
      if (errorMessage.includes('pending delete request already exists')) {
        // Close the delete request modal first
        setShowDeleteRequestModal(false);
        setIsDeleting(false);
        // Show alert modal with the duplicate request message
        setAlertModal({
          isOpen: true,
          title: t('deleteRequests.requestDeletion'),
          message: t('deleteRequests.pendingRequestExists'),
          type: 'warning',
        });
      } else {
        // For other errors, the DeleteRequestModal will show them inline
        setIsDeleting(false);
      }
    }
  };

  const canEdit = (): boolean => {
    if (!user) {
      return false;
    }
    return user.role === SalesRole.ADMIN || user.role === SalesRole.OFFICER;
  };

  const canRequestDelete = (): boolean => {
    if (!user) {
      return false;
    }
    return user.role === SalesRole.ADMIN || user.role === SalesRole.OFFICER;
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
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        <button onClick={onBack} className="btn-secondary flex items-center gap-3 sm:w-auto">
          <ArrowLeft size={20} />
          {t('customers.detail.back')}
        </button>
        <div className="flex-1">
          <h1 className="text-headline-2 mb-1">{t('customers.detail.customerDetails')}</h1>
          <p className="text-body-2">{t('customers.detail.customerDetails')}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-stretch">
        <div className="lg:col-span-2 flex">
          <div className="card-elevated flex-1 flex flex-col" id="customer-info-card">
            <div className="card-header flex-shrink-0">
              <h2 className="text-headline-5">{t('customers.detail.customerDetails')}</h2>
            </div>
            <div className="card-content flex-grow">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-6 mb-8">
                <div className="flex-1">
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
                    <h2 className="text-headline-3 mb-3">{customer.name}</h2>
                  )}
                  <StatusBadge key={customer.currentStatus} status={customer.currentStatus} />
                </div>
              
                <div className="flex flex-col sm:flex-row gap-3">
                  {isEditing ? (
                    <>
                      <button
                        onClick={handleSave}
                        disabled={updating}
                        className="btn-primary flex items-center justify-center gap-3"
                      >
                        <Save size={18} />
                        {updating ? t('customers.form.updating') : t('customers.form.update')}
                      </button>
                      <button
                        onClick={handleCancelEdit}
                        className="btn-secondary flex items-center justify-center gap-3"
                      >
                        <X size={18} />
                        {t('customers.form.cancel')}
                      </button>
                    </>
                  ) : (
                    <>
                      {canEdit() && (
                        <button onClick={handleEdit} className="btn-outline flex items-center justify-center gap-3">
                          <Edit size={18} />
                          {t('customers.detail.edit')}
                        </button>
                      )}
                      {canRequestDelete() && (
                        <button
                          onClick={() => setShowDeleteRequestModal(true)}
                          disabled={isDeleting}
                          className="btn-outline flex items-center justify-center gap-3 text-orange-600 hover:text-orange-700 hover:bg-orange-50"
                        >
                          <Trash2 size={18} />
                          {t('deleteRequests.requestDeletion')}
                        </button>
                      )}
                    </>
                  )}
                </div>
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
                      <Building2 size={18} className="text-surface-500" />
                      {t('customers.form.company')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.company}
                        onChange={(e) => setEditForm(prev => ({ ...prev, company: e.target.value }))}
                        className="input-field"
                        placeholder={t('customers.form.company')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.company || <span className="text-surface-400 italic">{t('customers.form.company')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <UserCircle size={18} className="text-surface-500" />
                      {t('customers.form.salesPerson')}
                    </label>
                    <p className="text-body-1">{getSalesPersonDisplayName(customer.salesPhone || '')}</p>
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Calendar size={18} className="text-surface-500" />
                      {t('customers.form.certifiedAt')}
                    </label>
                    {isEditing ? (
                      <input
                        type="date"
                        value={editForm.certifiedAt ? editForm.certifiedAt.split('T')[0] : ''}
                        onChange={(e) => setEditForm(prev => ({ ...prev, certifiedAt: e.target.value ? `${e.target.value}T00:00:00Z` : undefined }))}
                        className="input-field"
                        placeholder={t('customers.form.certifiedAt.placeholder')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.certifiedAt ? new Date(customer.certifiedAt).toLocaleDateString() : <span className="text-surface-400 italic">{t('customers.form.certifiedAt.placeholder')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <MapPin size={18} className="text-surface-500" />
                      {t('customers.form.location')}
                    </label>
                    {isEditing ? (
                      <div>
                        <div className="flex gap-2">
                          <input
                            type="text"
                            value={editForm.location}
                            onChange={(e) => handleEditFormChange('location', e.target.value)}
                            className="input-field flex-1"
                            placeholder={t('customers.form.location')}
                            readOnly
                          />
                          <button
                            type="button"
                            onClick={handleOpenMapPicker}
                            className="btn-outline flex items-center gap-2 px-3"
                          >
                            <MapPin size={16} />
                            {t('map.location')}
                          </button>
                        </div>
                        {selectedLocation && (
                          <div className="mt-2 p-2 bg-green-50 border border-green-200 rounded text-sm text-green-800">
                            <strong>{t('map.selectedLocation')}:</strong> {selectedLocation.address}
                            <br />
                            <span className="text-xs">{t('map.location')}: {selectedLocation.latitude.toFixed(6)}, {selectedLocation.longitude.toFixed(6)}</span>
                          </div>
                        )}
                      </div>
                    ) : (
                      <p className="text-body-1">{customer.location || <span className="text-surface-400 italic">{t('customers.form.location')}</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <DollarSign size={18} className="text-surface-500" />
                      {t('customers.form.price')}
                    </label>
                    {isEditing ? (
                      <input
                        type="number"
                        value={editForm.price || ''}
                        onChange={(e) => setEditForm(prev => ({ ...prev, price: e.target.value ? parseFloat(e.target.value) : undefined }))}
                        className="input-field"
                        placeholder={t('customers.form.price')}
                        min="0"
                        step="0.01"
                      />
                    ) : (
                      <p className="text-body-1">
                        {customer.price !== undefined && customer.price !== null 
                          ? `$${customer.price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` 
                          : <span className="text-surface-400 italic">{t('customers.form.price')}</span>
                        }
                      </p>
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
                        <option value="">Not specified</option>
                        <option value="male">Male</option>
                        <option value="female">Female</option>
                        <option value="other">Other</option>
                      </select>
                    ) : (
                      <p className="text-body-1">{customer.gender || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Briefcase size={18} className="text-surface-500" />
                      {t('customers.form.businessType')}
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.businessType}
                        onChange={(e) => setEditForm(prev => ({ ...prev, businessType: e.target.value }))}
                        className="input-field"
                        placeholder={t('customers.form.businessType')}
                      />
                    ) : (
                      <p className="text-body-1">{customer.businessType || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label">
                      {t('customers.form.businessReq')}
                    </label>
                    {isEditing ? (
                      <textarea
                        value={editForm.businessRequirements}
                        onChange={(e) => setEditForm(prev => ({ ...prev, businessRequirements: e.target.value }))}
                        rows={4}
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
          {/* Status Management Card */}
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
                    {customer.currentStatus === 'BUSINESS_DONE' 
                      ? t('customers.detail.businessComplete')
                      : t('customers.detail.loadingTransitions')}
                  </p>
                </div>
              )}
            </div>
          </div>

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

      {/* Map Picker Modal */}
      <GaodeMapPicker
        isOpen={showMapPicker}
        onClose={handleCloseMapPicker}
        onSelect={handleLocationSelect}
        initialLocation={selectedLocation || undefined}
      />

      {/* Delete Request Modal for Officers */}
      {customer && (
        <>
          <DeleteRequestModal
            isOpen={showDeleteRequestModal}
            onClose={() => setShowDeleteRequestModal(false)}
            onSubmit={handleDeleteRequest}
            customerName={customer.name}
          />

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