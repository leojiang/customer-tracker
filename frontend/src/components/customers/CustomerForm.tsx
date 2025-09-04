'use client';

import { useState } from 'react';
import { X, Save, User, Phone, Building2, MapPin, GraduationCap, Briefcase } from 'lucide-react';
import { CreateCustomerRequest, CustomerStatus } from '@/types/customer';
import { customerApi } from '@/lib/api';

interface CustomerFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CustomerForm({ onClose, onSuccess }: CustomerFormProps) {
  const [formData, setFormData] = useState<CreateCustomerRequest>({
    name: '',
    phone: '',
    company: '',
    businessRequirements: '',
    businessType: '',
    age: undefined,
    education: '',
    gender: '',
    location: '',
    currentStatus: CustomerStatus.CUSTOMER_CALLED,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name.trim() || !formData.phone.trim()) {
      setError('Name and phone are required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const cleanedData: CreateCustomerRequest = {
        ...formData,
        company: formData.company?.trim() || undefined,
        businessRequirements: formData.businessRequirements?.trim() || undefined,
        businessType: formData.businessType?.trim() || undefined,
        education: formData.education?.trim() || undefined,
        gender: formData.gender?.trim() || undefined,
        location: formData.location?.trim() || undefined,
      };
      
      await customerApi.createCustomer(cleanedData);
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create customer');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof CreateCustomerRequest, value: string | number | undefined) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (error) {
      setError(null);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-gray-900">Add New Customer</h2>
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
                  Full Name *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  className="input-field focus-ring"
                  placeholder="Enter customer full name"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Phone size={16} className="inline mr-1" />
                  Phone *
                </label>
                <input
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => handleInputChange('phone', e.target.value)}
                  className="input-field"
                  placeholder="Enter phone number"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Building2 size={16} className="inline mr-1" />
                  Company
                </label>
                <input
                  type="text"
                  value={formData.company}
                  onChange={(e) => handleInputChange('company', e.target.value)}
                  className="input-field"
                  placeholder="Enter company name"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <MapPin size={16} className="inline mr-1" />
                  Location
                </label>
                <input
                  type="text"
                  value={formData.location}
                  onChange={(e) => handleInputChange('location', e.target.value)}
                  className="input-field"
                  placeholder="Enter location"
                />
              </div>
            </div>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Age
                </label>
                <input
                  type="number"
                  value={formData.age || ''}
                  onChange={(e) => handleInputChange('age', e.target.value ? parseInt(e.target.value) : undefined)}
                  className="input-field"
                  placeholder="Enter age"
                  min="1"
                  max="120"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Gender
                </label>
                <select
                  value={formData.gender}
                  onChange={(e) => handleInputChange('gender', e.target.value)}
                  className="input-field"
                >
                  <option value="">Select gender</option>
                  <option value="male">Male</option>
                  <option value="female">Female</option>
                  <option value="other">Other</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <GraduationCap size={16} className="inline mr-1" />
                  Education
                </label>
                <input
                  type="text"
                  value={formData.education}
                  onChange={(e) => handleInputChange('education', e.target.value)}
                  className="input-field"
                  placeholder="Enter education level"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Briefcase size={16} className="inline mr-1" />
                  Business Type
                </label>
                <input
                  type="text"
                  value={formData.businessType}
                  onChange={(e) => handleInputChange('businessType', e.target.value)}
                  className="input-field"
                  placeholder="Enter business type"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Business Requirements
            </label>
            <textarea
              value={formData.businessRequirements}
              onChange={(e) => handleInputChange('businessRequirements', e.target.value)}
              rows={4}
              className="input-field"
              placeholder="Enter business requirements or notes..."
            />
          </div>

          <div className="divider"></div>
          
          <div className="flex flex-col sm:flex-row justify-end gap-4">
            <button 
              type="button"
              onClick={onClose}
              className="btn-secondary flex items-center justify-center gap-3 sm:w-auto"
            >
              Cancel
            </button>
            <button 
              type="submit"
              disabled={loading}
              className="btn-primary flex items-center justify-center gap-3 sm:w-auto"
            >
              <Save size={18} />
              {loading ? 'Creating Customer...' : 'Create Customer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}