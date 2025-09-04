'use client';

import { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Edit, Phone, Building2, MapPin, User, GraduationCap, Briefcase, Save, X } from 'lucide-react';
import { Customer, CustomerStatus, StatusTransitionRequest, UpdateCustomerRequest, CustomerStatusDisplayNames } from '@/types/customer';
import { customerApi } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import StatusHistory from '@/components/customers/StatusHistory';
// import { format } from 'date-fns'; // Unused import removed

interface CustomerDetailProps {
  customerId: string;
  onBack: () => void;
}

export default function CustomerDetail({ customerId, onBack }: CustomerDetailProps) {
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
    education: '',
    gender: '',
    location: '',
  });
  const [statusTransition, setStatusTransition] = useState<StatusTransitionRequest>({
    toStatus: CustomerStatus.CUSTOMER_CALLED,
    reason: '',
  });
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [historyRefreshTrigger, setHistoryRefreshTrigger] = useState(0);
  const [validTransitions, setValidTransitions] = useState<CustomerStatus[]>([]);

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
        education: data.education || '',
        gender: data.gender || '',
        location: data.location || '',
      });
      // Load valid transitions after customer is loaded
      await loadValidTransitions();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load customer');
    } finally {
      setLoading(false);
    }
  }, [customerId, loadValidTransitions]);

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
        education: customer.education || '',
        gender: customer.gender || '',
        location: customer.location || '',
      });
    }
    setIsEditing(false);
  };

  const handleSave = async () => {
    if (!customer) {
      return;
    }
    
    try {
      setUpdating(true);
      const updatedCustomer = await customerApi.updateCustomer(customer.id, editForm);
      setCustomer(updatedCustomer);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update customer');
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
      setError(err instanceof Error ? err.message : 'Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  const getAvailableStatusTransitions = (): CustomerStatus[] => {
    // Return valid transitions from backend validation
    return validTransitions;
  };

  if (loading) {
    return (
      <div className="card p-6 text-center">
        <div className="text-gray-600">Loading customer details...</div>
      </div>
    );
  }

  if (error || !customer) {
    return (
      <div className="card p-6 text-center">
        <div className="text-red-600 mb-4">Error loading customer</div>
        <p className="text-gray-600 mb-4">{error || 'Customer not found'}</p>
        <div className="flex gap-2 justify-center">
          <button onClick={onBack} className="btn-secondary">
            Go Back
          </button>
          <button onClick={loadCustomer} className="btn-primary">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  const availableTransitions = getAvailableStatusTransitions();

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        <button onClick={onBack} className="btn-secondary flex items-center gap-3 sm:w-auto">
          <ArrowLeft size={20} />
          Back to List
        </button>
        <div className="flex-1">
          <h1 className="text-headline-2 mb-1">Customer Details</h1>
          <p className="text-body-2">Manage customer information and track status changes</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-stretch">
        <div className="lg:col-span-2 flex">
          <div className="card-elevated flex-1 flex flex-col" id="customer-info-card">
            <div className="card-header flex-shrink-0">
              <h2 className="text-headline-5">Customer Information</h2>
            </div>
            <div className="card-content flex-grow">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-6 mb-8">
                <div className="flex-1">
                  {isEditing ? (
                    <div>
                      <label className="input-label">Customer Name</label>
                      <input
                        type="text"
                        value={editForm.name}
                        onChange={(e) => setEditForm(prev => ({ ...prev, name: e.target.value }))}
                        className="input-field text-headline-4 font-medium"
                      />
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
                        {updating ? 'Saving...' : 'Save Changes'}
                      </button>
                      <button 
                        onClick={handleCancelEdit}
                        className="btn-secondary flex items-center justify-center gap-3"
                      >
                        <X size={18} />
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button onClick={handleEdit} className="btn-outline flex items-center justify-center gap-3">
                      <Edit size={18} />
                      Edit Details
                    </button>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="space-y-6">
                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Phone size={18} className="text-surface-500" />
                      Phone Number
                    </label>
                    {isEditing ? (
                      <input
                        type="tel"
                        value={editForm.phone}
                        onChange={(e) => setEditForm(prev => ({ ...prev, phone: e.target.value }))}
                        className="input-field"
                        placeholder="Enter phone number"
                      />
                    ) : (
                      <p className="text-body-1">{customer.phone}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <Building2 size={18} className="text-surface-500" />
                      Company
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.company}
                        onChange={(e) => setEditForm(prev => ({ ...prev, company: e.target.value }))}
                        className="input-field"
                        placeholder="Enter company name"
                      />
                    ) : (
                      <p className="text-body-1">{customer.company || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <MapPin size={18} className="text-surface-500" />
                      Location
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.location}
                        onChange={(e) => setEditForm(prev => ({ ...prev, location: e.target.value }))}
                        className="input-field"
                        placeholder="Enter location"
                      />
                    ) : (
                      <p className="text-body-1">{customer.location || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>
                </div>

                <div className="space-y-6">
                  <div>
                    <label className="input-label flex items-center gap-2">
                      <User size={18} className="text-surface-500" />
                      Age
                    </label>
                    {isEditing ? (
                      <input
                        type="number"
                        value={editForm.age || ''}
                        onChange={(e) => setEditForm(prev => ({ ...prev, age: e.target.value ? parseInt(e.target.value) : undefined }))}
                        className="input-field"
                        placeholder="Enter age"
                      />
                    ) : (
                      <p className="text-body-1">{customer.age || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <GraduationCap size={18} className="text-surface-500" />
                      Education
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.education}
                        onChange={(e) => setEditForm(prev => ({ ...prev, education: e.target.value }))}
                        className="input-field"
                        placeholder="Enter education level"
                      />
                    ) : (
                      <p className="text-body-1">{customer.education || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label flex items-center gap-2">
                      <User size={18} className="text-surface-500" />
                      Gender
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
                      Business Type
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editForm.businessType}
                        onChange={(e) => setEditForm(prev => ({ ...prev, businessType: e.target.value }))}
                        className="input-field"
                        placeholder="Enter business type"
                      />
                    ) : (
                      <p className="text-body-1">{customer.businessType || <span className="text-surface-400 italic">Not specified</span>}</p>
                    )}
                  </div>

                  <div>
                    <label className="input-label">
                      Business Requirements
                    </label>
                    {isEditing ? (
                      <textarea
                        value={editForm.businessRequirements}
                        onChange={(e) => setEditForm(prev => ({ ...prev, businessRequirements: e.target.value }))}
                        rows={4}
                        className="input-field"
                        placeholder="Enter business requirements..."
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
              <h3 className="text-headline-6">Status Management</h3>
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
                  Update Status
                </button>
              ) : (
                <div className="text-center py-6">
                  <div className="text-body-2 text-surface-500 mb-2">No status transitions available</div>
                  <p className="text-caption text-surface-400">
                    {customer.currentStatus === 'BUSINESS_DONE' 
                      ? 'Business process is complete - no further transitions allowed'
                      : 'Loading available transitions...'}
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
                Update Status
              </h3>
            </div>
            
            <div className="card-content space-y-6">
              <div>
                <label className="input-label">
                  New Status
                </label>
                <select
                  value={statusTransition.toStatus}
                  onChange={(e) => setStatusTransition(prev => ({ ...prev, toStatus: e.target.value as CustomerStatus }))}
                  className="input-field focus-ring"
                >
                  {availableTransitions.map(status => (
                    <option key={status} value={status}>
                      {CustomerStatusDisplayNames[status]}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="input-label">
                  Reason (Optional)
                </label>
                <textarea
                  value={statusTransition.reason}
                  onChange={(e) => setStatusTransition(prev => ({ ...prev, reason: e.target.value }))}
                  rows={3}
                  className="input-field focus-ring"
                  placeholder="Enter reason for status change..."
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
                  {updating ? 'Updating...' : 'Update Status'}
                </button>
                <button 
                  onClick={() => setShowStatusModal(false)}
                  className="btn-secondary flex items-center justify-center gap-3 flex-1"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}