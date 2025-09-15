'use client';

import { useState } from 'react';
import { X, Save, User, Phone, Building2, MapPin, GraduationCap, Briefcase, DollarSign, AlertCircle } from 'lucide-react';
import { CreateCustomerRequest, CustomerStatus, EducationLevel, EducationLevelDisplayNames, getTranslatedEducationLevelName } from '@/types/customer';
import { customerApi } from '@/lib/api';
import { validatePhoneNumber, validateName, validateAge, formatPhoneNumber } from '@/lib/validation';
import GaodeMapPicker, { LocationData } from '@/components/ui/GaodeMapPicker';
import { useLanguage } from '@/contexts/LanguageContext';

interface CustomerFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CustomerForm({ onClose, onSuccess }: CustomerFormProps) {
  const { t } = useLanguage();
  const [formData, setFormData] = useState<CreateCustomerRequest>({
    name: '',
    phone: '',
    company: '',
    businessRequirements: '',
    businessType: '',
    age: undefined,
    education: undefined as EducationLevel | undefined,
    gender: '',
    location: '',
    price: undefined,
    currentStatus: CustomerStatus.CUSTOMER_CALLED,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [showMapPicker, setShowMapPicker] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(null);

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
        company: formData.company?.trim() || undefined,
        businessRequirements: formData.businessRequirements?.trim() || undefined,
        businessType: formData.businessType?.trim() || undefined,
        education: formData.education,
        gender: formData.gender?.trim() || undefined,
        location: selectedLocation ? `${selectedLocation.address} (${selectedLocation.latitude.toFixed(6)}, ${selectedLocation.longitude.toFixed(6)})` : formData.location?.trim() || undefined,
      };
      
      await customerApi.createCustomer(cleanedData);
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('customers.form.failedCreate'));
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
    setFormData(prev => ({ ...prev, location: location.address }));
  };

  const handleOpenMapPicker = () => {
    setShowMapPicker(true);
  };

  const handleCloseMapPicker = () => {
    setShowMapPicker(false);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4">
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

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="space-y-6">
              <div>
                <label className="input-label flex items-center gap-2">
                  <User size={18} className="text-surface-500" />
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

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Phone size={16} className="inline mr-1" />
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

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Building2 size={16} className="inline mr-1" />
                  {t('customers.form.company')}
                </label>
                <input
                  type="text"
                  value={formData.company}
                  onChange={(e) => handleInputChange('company', e.target.value)}
                  className="input-field"
                  placeholder={t('customers.form.company')}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <MapPin size={16} className="inline mr-1" />
                  {t('customers.form.location')}
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={formData.location}
                    onChange={(e) => handleInputChange('location', e.target.value)}
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

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <DollarSign size={16} className="inline mr-1" />
                  {t('customers.form.price')}
                </label>
                <input
                  type="number"
                  value={formData.price || ''}
                  onChange={(e) => handleInputChange('price', e.target.value ? parseFloat(e.target.value) : undefined)}
                  className="input-field"
                  placeholder={t('customers.form.price')}
                  min="0"
                  step="0.01"
                />
              </div>
            </div>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
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

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
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
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <GraduationCap size={16} className="inline mr-1" />
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
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Briefcase size={16} className="inline mr-1" />
                  {t('customers.form.businessType')}
                </label>
                <input
                  type="text"
                  value={formData.businessType}
                  onChange={(e) => handleInputChange('businessType', e.target.value)}
                  className="input-field"
                  placeholder={t('customers.form.businessType')}
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t('customers.form.businessReq')}
            </label>
            <textarea
              value={formData.businessRequirements}
              onChange={(e) => handleInputChange('businessRequirements', e.target.value)}
              rows={4}
              className="input-field"
              placeholder={t('customers.form.businessReq')}
            />
          </div>

          <div className="divider"></div>
          
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

      {/* Map Picker Modal */}
      <GaodeMapPicker
        isOpen={showMapPicker}
        onClose={handleCloseMapPicker}
        onSelect={handleLocationSelect}
        initialLocation={selectedLocation || undefined}
      />
    </div>
  );
}