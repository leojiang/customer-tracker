'use client';

import { useState, useEffect } from 'react';
import { Search, Plus, Phone, Building2, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { Customer, CustomerSearchParams, CustomerPageResponse } from '@/types/customer';
import { customerApi } from '@/lib/api';
import StatusBadge from '@/components/ui/StatusBadge';
import { format } from 'date-fns';

interface CustomerListProps {
  onCustomerSelect?: (customer: Customer) => void;
  onCreateCustomer?: () => void;
}

export default function CustomerList({ onCustomerSelect, onCreateCustomer }: CustomerListProps) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<CustomerSearchParams>({
    page: 1,
    limit: 10,
  });
  const [pageInfo, setPageInfo] = useState({
    total: 0,
    totalPages: 0,
    page: 1,
    limit: 10,
  });
  const [searchTerm, setSearchTerm] = useState('');

  const loadCustomers = async (params: CustomerSearchParams) => {
    try {
      setLoading(true);
      setError(null);
      const response: CustomerPageResponse = await customerApi.searchCustomers(params);
      setCustomers(response.items);
      setPageInfo({
        total: response.total,
        totalPages: response.totalPages,
        page: response.page,
        limit: response.limit,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCustomers(searchParams);
  }, [searchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchParams(prev => ({
      ...prev,
      q: searchTerm.trim() || undefined,
      page: 1,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams(prev => ({ ...prev, page: newPage }));
  };

  const handleCustomerClick = (customer: Customer) => {
    if (onCustomerSelect) {
      onCustomerSelect(customer);
    }
  };

  if (error) {
    return (
      <div className="card p-6 text-center">
        <div className="text-red-600 mb-4">Error loading customers</div>
        <p className="text-gray-600 mb-4">{error}</p>
        <button 
          onClick={() => loadCustomers(searchParams)}
          className="btn-primary"
        >
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
        <div>
          <h1 className="text-headline-2 mb-2">Customer Management</h1>
          <p className="text-body-2">Manage and track your customer relationships</p>
        </div>
        <button 
          onClick={onCreateCustomer}
          className="btn-primary flex items-center justify-center gap-3 sm:w-auto"
        >
          <Plus size={20} />
          Add Customer
        </button>
      </div>

      {/* Search Card */}
      <div className="card-elevated">
        <div className="card-header">
          <h2 className="text-headline-6">Search & Filter</h2>
        </div>
        <div className="card-content">
          <form onSubmit={handleSearch}>
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-surface-400" size={20} />
                  <input
                    type="text"
                    placeholder="Search by name, phone, or company..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="input-field pl-12"
                  />
                </div>
              </div>
              <button type="submit" className="btn-primary md:w-auto">
                <Search size={18} className="mr-2" />
                Search
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Results Card */}
      <div className="card-elevated">{loading ? (
        <div className="card-content text-center py-12">
          <div className="loading-skeleton w-16 h-16 rounded-full mx-auto mb-4"></div>
          <div className="text-body-1 text-surface-600">Loading customers...</div>
        </div>
      ) : customers.length === 0 ? (
        <div className="card-content text-center py-12">
          <div className="w-16 h-16 bg-surface-200 rounded-full mx-auto mb-4 flex items-center justify-center">
            <Phone size={24} className="text-surface-400" />
          </div>
          <div className="text-headline-6 mb-2">No customers found</div>
          <p className="text-body-2 mb-6">
            {searchTerm ? 'Try adjusting your search terms' : 'Get started by adding your first customer'}
          </p>
          {searchTerm && (
            <button 
              onClick={() => {
                setSearchTerm('');
                setSearchParams(prev => ({ ...prev, q: undefined, page: 1 }));
              }}
              className="btn-outline"
            >
              Clear Search
            </button>
          )}
        </div>

      ) : (
        <>
          <div className="card-header">
            <div className="flex justify-between items-center">
              <h2 className="text-headline-6">
                {pageInfo.total} Customer{pageInfo.total !== 1 ? 's' : ''} Found
              </h2>
              <div className="text-body-2">
                Page {pageInfo.page} of {pageInfo.totalPages}
              </div>
            </div>
          </div>
          <div className="divide-y divide-surface-100">
            {customers.map((customer) => (
              <div
                key={customer.id}
                onClick={() => handleCustomerClick(customer)}
                className="list-item-interactive group"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex justify-between items-start mb-3">
                    <div className="flex-1 min-w-0">
                      <h3 className="text-headline-6 mb-2 group-hover:text-primary-600 transition-colors">
                        {customer.name}
                      </h3>
                      <div className="flex flex-wrap items-center gap-4 text-body-2">
                        <div className="flex items-center gap-1.5">
                          <Phone size={16} className="text-surface-500" />
                          <span>{customer.phone}</span>
                        </div>
                        {customer.company && (
                          <div className="flex items-center gap-1.5">
                            <Building2 size={16} className="text-surface-500" />
                            <span>{customer.company}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-1.5">
                          <Calendar size={16} className="text-surface-500" />
                          <span>{format(new Date(customer.createdAt), 'MMM dd, yyyy')}</span>
                        </div>
                      </div>
                    </div>
                    <div className="ml-4 flex-shrink-0">
                      <StatusBadge status={customer.currentStatus} />
                    </div>
                  </div>
                  
                  {customer.businessRequirements && (
                    <p className="text-body-2 text-surface-600 line-clamp-2 mt-2">
                      {customer.businessRequirements}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>

        </>
      )}
      </div>

      {/* Pagination */}
      {pageInfo.totalPages > 1 && (
        <div className="card">
          <div className="card-content">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div className="text-body-2">
                Showing {((pageInfo.page - 1) * pageInfo.limit) + 1} to {Math.min(pageInfo.page * pageInfo.limit, pageInfo.total)} of {pageInfo.total} customers
              </div>
              
              <div className="flex items-center justify-center sm:justify-end gap-2">
                <button
                  onClick={() => handlePageChange(pageInfo.page - 1)}
                  disabled={pageInfo.page <= 1}
                  className="btn-secondary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronLeft size={18} />
                  Previous
                </button>
                
                <div className="flex items-center gap-2 px-4">
                  <span className="text-body-2">
                    Page {pageInfo.page} of {pageInfo.totalPages}
                  </span>
                </div>
                
                <button
                  onClick={() => handlePageChange(pageInfo.page + 1)}
                  disabled={pageInfo.page >= pageInfo.totalPages}
                  className="btn-secondary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                  <ChevronRight size={18} />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}