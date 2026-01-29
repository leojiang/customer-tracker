'use client';

import { useState } from 'react';
import { X, Save, User, Phone, Building2, MapPin, GraduationCap, Briefcase, AlertCircle, Lock, Calendar, IdCard } from 'lucide-react';
import { CreateCustomerRequest, CustomerStatus, EducationLevel, EducationLevelDisplayNames, getTranslatedEducationLevelName, CertificateType, CertificateTypeTranslationKeys, CertificateIssuer, CertificateIssuerTranslationKeys } from '@/types/customer';
import { customerApi } from '@/lib/api';
import { validatePhoneNumber, validateName, validateAge, formatPhoneNumber } from '@/lib/validation';
import { getErrorMessage } from '@/lib/errorHandler';
import { getCertificateIssuerOptions } from '@/lib/certificateIssuerUtils';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { SalesRole } from '@/types/auth';

interface CustomerFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CustomerForm({ onClose, onSuccess }: CustomerFormProps) {
  const { t } = useLanguage();
  const { user } = useAuth();

  // Check if user has permission to add customers
  const canAddCustomer = user?.role === SalesRole.ADMIN || user?.role === SalesRole.CUSTOMER_AGENT;

  const [formData, setFormData] = useState<CreateCustomerRequest>({
    name: '',
    phone: '',
    certificateIssuer: CertificateIssuer.OTHER,
    businessRequirements: '',
    certificateType: undefined as CertificateType | undefined,
    age: undefined,
    education: undefined as EducationLevel | undefined,
    gender: '',
    address: '',
    idCard: undefined,
    currentStatus: CustomerStatus.NEW,
    customerAgent: user?.name || '',
    certifiedAt: undefined,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [idCardError, setIdCardError] = useState<string>('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Clear previous errors
    setError(null);
    setFieldErrors({});

    // Validate required fields
    const nameValidation = validateName(formData.name);
    const phoneValidation = validatePhoneNumber(formData.phone);
    const ageValidation = validateAge(formData.age);

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

    if (!formData.certificateType) {
      newFieldErrors.certificateType = t('validation.certificateTypeRequired');
    }

    if (Object.keys(newFieldErrors).length > 0) {
      setFieldErrors(newFieldErrors);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const cleanedData: CreateCustomerRequest = {
        ...formData,
        phone: formatPhoneNumber(formData.phone), // Format phone number
        certificateIssuer: formData.certificateIssuer?.trim() || undefined,
        businessRequirements: formData.businessRequirements?.trim() || undefined,
        certificateType: formData.certificateType,
        education: formData.education,
        gender: formData.gender?.trim() || undefined,
        address: formData.address?.trim() || undefined,
      };

      await customerApi.createCustomer(cleanedData);
      onSuccess();
    } catch (err) {
      // Use the error handler to map business error codes to user-friendly messages
      const userErrorMessage = getErrorMessage(err, t);
      setError(userErrorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof CreateCustomerRequest, value: string | number | undefined) => {
    setFormData(prev => ({ ...prev, [field]: value }));

    // Clear errors when user starts typing
    if (error) {
      setError(null);
    }

    // Clear field-specific errors
    if (fieldErrors[field]) {
      setFieldErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validateField = (field: keyof CreateCustomerRequest, value: string | number | undefined) => {
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
      case 'certificateType':
        if (!value) {
          setFieldErrors(prev => ({ ...prev, [field]: t('validation.certificateTypeRequired') }));
        } else {
          setFieldErrors(prev => {
            const newErrors = { ...prev };
            delete newErrors[field];
            return newErrors;
          });
        }
        return;
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

  // Show permission denied message for CUSTOMER_AGENT
  if (!canAddCustomer) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <div className="flex flex-col items-center justify-center text-center py-8">
            <div className="bg-orange-100 rounded-full p-4 mb-4">
              <Lock size={32} className="text-orange-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {t('error.permissionDenied')}
            </h3>
            <p className="text-sm text-gray-500 mb-6">
              {t('error.onlyAdminOfficerCanAdd')}
            </p>
            <button
              onClick={onClose}
              className="btn-primary flex items-center justify-center gap-2"
            >
              {t('customers.close')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div className="flex-shrink-0 border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-gray-900">{t('customers.form.createCustomer')}</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              <X size={24} />
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="space-y-6">
              {/* Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <User size={16} className="text-surface-500" />
                  {t('customers.form.name')} *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  onBlur={(e) => validateField('name', e.target.value)}
                  className={`input-field focus-ring ${fieldErrors.name ? 'border-red-500' : ''}`}
                  placeholder={t('customers.form.name')}
                  required
                />
                {fieldErrors.name && (
                  <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                    <AlertCircle size={14} />
                    <span>{fieldErrors.name}</span>
                  </div>
                )}
              </div>

              {/* Phone */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Phone size={16} className="text-surface-500" />
                  {t('customers.form.phone')} *
                </label>
                <input
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => handleInputChange('phone', e.target.value)}
                  onBlur={(e) => validateField('phone', e.target.value)}
                  className={`input-field ${fieldErrors.phone ? 'border-red-500' : ''}`}
                  placeholder={t('customers.form.phone')}
                  required
                />
                {fieldErrors.phone && (
                  <div className="flex items-center gap-1 mt-1 text-red-600 text-sm">
                    <AlertCircle size={14} />
                    <span>{fieldErrors.phone}</span>
                  </div>
                )}
              </div>

              {/* ID Card */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <IdCard size={16} className="text-surface-500" />
                  {t('customers.form.idCard')}
                </label>
                <input
                  type="text"
                  value={formData.idCard || ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    // Check if value contains only digits and English letters
                    if (value === '' || /^[a-zA-Z0-9]*$/.test(value)) {
                      handleInputChange('idCard', value || undefined);
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
              </div>

              {/* Customer Agent */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <User size={16} className="text-surface-500" />
                  {t('customers.form.customerAgent')}
                </label>
                <input
                  type="text"
                  value={formData.customerAgent || ''}
                  onChange={(e) => handleInputChange('customerAgent', e.target.value)}
                  className="input-field"
                  placeholder={t('customers.form.customerAgent.placeholder')}
                />
              </div>

              {/* Certificate Issuer */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Building2 size={16} className="text-surface-500" />
                  {t('customers.form.certificateIssuer')}
                </label>
                <select
                  value={formData.certificateIssuer || ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    const certificateIssuerValue = value && value !== '' ? value as CertificateIssuer : CertificateIssuer.OTHER;
                    handleInputChange('certificateIssuer', certificateIssuerValue);
                  }}
                  className="input-field"
                >
                  {getCertificateIssuerOptions().map((option) => (
                    <option key={option.value} value={option.value}>
                      {t(CertificateIssuerTranslationKeys[option.value])}
                    </option>
                  ))}
                </select>
              </div>

              {/* Location */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <MapPin size={16} className="text-surface-500" />
                  {t('customers.form.address')}
                </label>
                <input
                  type="text"
                  value={formData.address}
                  onChange={(e) => handleInputChange('address', e.target.value)}
                  className="input-field"
                  placeholder={t('customers.form.address')}
                />
              </div>
            </div>

            <div className="space-y-6">
              {/* Age */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Calendar size={16} className="text-surface-500" />
                  {t('customers.form.age')}
                </label>
                <input
                  type="number"
                  value={formData.age || ''}
                  onChange={(e) => handleInputChange('age', e.target.value ? parseInt(e.target.value) : undefined)}
                  onBlur={(e) => validateField('age', e.target.value ? parseInt(e.target.value) : undefined)}
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

              {/* Gender */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <User size={16} className="text-surface-500" />
                  {t('customers.form.gender')}
                </label>
                <select
                  value={formData.gender}
                  onChange={(e) => handleInputChange('gender', e.target.value)}
                  className="input-field"
                >
                  <option value="">{t('customers.form.gender.select')}</option>
                  <option value="male">{t('customers.form.gender.male')}</option>
                  <option value="female">{t('customers.form.gender.female')}</option>
                  <option value="other">{t('customers.form.gender.other')}</option>
                </select>
                {/* Placeholder to maintain alignment with Phone field */}
                <div className="h-1"></div>
              </div>

              {/* Education */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <GraduationCap size={16} className="text-surface-500" />
                  {t('customers.form.education')}
                </label>
                <select
                  value={formData.education || ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    const educationValue = value && value !== '' ? value as EducationLevel : undefined;
                    handleInputChange('education', educationValue);
                  }}
                  className="input-field"
                >
                  <option value="">{t('customers.form.education')}</option>
                  {Object.entries(EducationLevelDisplayNames).map(([key]) => (
                    <option key={key} value={key}>
                      {getTranslatedEducationLevelName(key as EducationLevel, t)}
                    </option>
                  ))}
                </select>
                <div className="h-1"></div>
              </div>

              {/* Certified At */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Calendar size={16} className="text-surface-500" />
                  {t('customers.form.certifiedAt')}
                </label>
                <input
                  type="date"
                  value={formData.certifiedAt || ''}
                  onChange={(e) => handleInputChange('certifiedAt', e.target.value || undefined)}
                  className="input-field"
                  placeholder={t('customers.form.certifiedAt.placeholder')}
                />
              </div>

              {/* Certificate Type */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Briefcase size={16} className="text-surface-500" />
                  {t('customers.form.certificateType')} *
                </label>
                <select
                  value={formData.certificateType || ''}
                  onChange={(e) => {
                    const value = e.target.value;
                    const certificateValue = value && value !== '' ? value as CertificateType : undefined;
                    handleInputChange('certificateType', certificateValue);
                    validateField('certificateType', certificateValue);
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
              </div>

              {/* Business Requirements */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                  <Briefcase size={16} className="text-surface-500" />
                  {t('customers.form.businessReq')}
                </label>
                <input
                  type="text"
                  value={formData.businessRequirements}
                  onChange={(e) => handleInputChange('businessRequirements', e.target.value)}
                  className="input-field"
                  placeholder={t('customers.form.businessReq')}
                />
              </div>
            </div>
          </div>

          <div className="divider"></div>
        </form>

        <div className="flex-shrink-0 border-t border-gray-200 px-6 py-4 bg-white">
          <form onSubmit={handleSubmit}>
            <div className="flex flex-col sm:flex-row justify-end gap-4">
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary flex items-center justify-center gap-3 sm:w-auto"
              >
                {t('customers.form.cancel')}
              </button>
              <button
                type="submit"
                disabled={loading}
                className="btn-primary flex items-center justify-center gap-3 sm:w-auto"
              >
                <Save size={18} />
                {loading ? t('customers.form.creating') : t('customers.form.create')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
