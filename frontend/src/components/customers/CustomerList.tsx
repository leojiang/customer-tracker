'use client';

import { useState, useEffect, useCallback } from 'react';
import { Search, Plus, Phone, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { Customer, CustomerSearchParams, CustomerPageResponse } from '@/types/customer';
import { customerApi } from '@/lib/api';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { format } from 'date-fns';
import { SalesRole } from '@/types/auth';

interface CustomerListProps {
  onCustomerSelect?: (customer: Customer) => void;
  onCreateCustomer?: () => void;
}

export default function CustomerList({ onCustomerSelect, onCreateCustomer }: CustomerListProps) {
  const { t } = useLanguage();
  const { user } = useAuth();
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

  const loadCustomers = useCallback(async (params: CustomerSearchParams) => {
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
      setError(err instanceof Error ? err.message : t('customers.errorLoading'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadCustomers(searchParams);
  }, [searchParams, loadCustomers]);

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

  // Generate page numbers for pagination
  const getPageNumbers = () => {
    const delta = 2; // Number of pages to show on each side of current page
    const range: number[] = [];
    const rangeWithDots: (number | string)[] = [];

    for (let i = Math.max(2, pageInfo.page - delta); i <= Math.min(pageInfo.totalPages - 1, pageInfo.page + delta); i++) {
      range.push(i);
    }

    if (pageInfo.page - delta > 2) {
      rangeWithDots.push(1, '...');
    } else {
      if (pageInfo.totalPages > 1) {rangeWithDots.push(1);}
    }

    rangeWithDots.push(...range);

    if (pageInfo.page + delta < pageInfo.totalPages - 1) {
      rangeWithDots.push('...', pageInfo.totalPages);
    } else {
      if (pageInfo.totalPages > 1) {rangeWithDots.push(pageInfo.totalPages);}
    }

    return rangeWithDots;
  };

  const handleCustomerClick = (customer: Customer) => {
    if (onCustomerSelect) {
      onCustomerSelect(customer);
    }
  };

  const getLocalizedGender = (gender: string | undefined) => {
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

  if (error) {
    return (
      <div className="card p-6 text-center">
        <div className="text-red-600 mb-4">{t('customers.errorLoading')}</div>
        <p className="text-gray-600 mb-4">{error}</p>
        <button 
          onClick={() => loadCustomers(searchParams)}
          className="btn-primary"
        >
          {t('customers.tryAgain')}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
        <div>
          <h1 className="text-headline-2 mb-2">{t('customers.management')}</h1>
          <p className="text-body-2">{t('customers.manageTrack')}</p>
        </div>
        {(user?.role === SalesRole.ADMIN || user?.role === SalesRole.OFFICER) && (
          <button
            onClick={onCreateCustomer}
            className="btn-primary flex items-center justify-center gap-3 sm:w-auto"
          >
            <Plus size={20} />
            {t('customers.addCustomer')}
          </button>
        )}
      </div>

      {/* Main Content: Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Column: Search & Filter */}
        <div className="lg:col-span-1">
          <div className="card-elevated sticky top-24">
            <div className="card-header">
              <h2 className="text-headline-6">{t('customers.searchFilter')}</h2>
            </div>
            <div className="card-content">
              <form onSubmit={handleSearch} className="space-y-4">
                {/* Search Input */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    {t('customers.search')}
                  </label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-surface-400" size={18} />
                    <input
                      type="text"
                      placeholder={t('customers.searchPlaceholder')}
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="input-field pl-10"
                    />
                  </div>
                </div>

                {/* Search Button */}
                <button type="submit" className="btn-primary w-full flex items-center justify-center gap-2">
                  <Search size={18} />
                  {t('customers.search')}
                </button>

                {/* Additional filters can be added here */}
                <div className="border-t pt-4">
                  <p className="text-sm text-surface-500 italic">
                    More filters coming soon...
                  </p>
                </div>
              </form>
            </div>
          </div>
        </div>

        {/* Right Column: Customer List */}
        <div className="lg:col-span-3">
        <div className="card-elevated">{loading ? (
          <div className="card-content text-center py-12">
            <div className="loading-skeleton w-16 h-16 rounded-full mx-auto mb-4"></div>
            <div className="text-body-1 text-surface-600">{t('customers.loadingCustomers')}</div>
          </div>
        ) : customers.length === 0 ? (
          <div className="card-content text-center py-12">
            <div className="w-16 h-16 bg-surface-200 rounded-full mx-auto mb-4 flex items-center justify-center">
              <Phone size={24} className="text-surface-400" />
            </div>
            <div className="text-headline-6 mb-2">{t('customers.noCustomersFound')}</div>
            <p className="text-body-2 mb-6">
              {searchTerm ? t('customers.adjustSearch') : t('customers.getStartedFirst')}
            </p>
            {searchTerm && (
              <button
                onClick={() => {
                setSearchTerm('');
                setSearchParams(prev => ({ ...prev, q: undefined, page: 1 }));
              }}
              className="btn-outline"
            >
              {t('customers.clearSearch')}
            </button>
          )}
        </div>

      ) : (
        <>
          <div className="card-header">
            <div className="flex justify-between items-center">
              <h2 className="text-headline-6">
                {t('customers.found')} {pageInfo.total} {pageInfo.total === 1 ? t('customers.customersFound') : t('customers.customersFoundPlural')}
              </h2>
              <div className="text-body-2">
                {t('customers.page')} {pageInfo.page} {t('customers.of')} {pageInfo.totalPages}
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
                  <h3 className="text-headline-6 mb-2 group-hover:text-primary-600 transition-colors">
                    {customer.name}
                  </h3>
                  <div className="flex items-center justify-between gap-4 text-body-2">
                    <div className="flex flex-wrap items-center gap-4">
                      <div className="flex items-center gap-1.5">
                        <Phone size={16} className="text-surface-500" />
                        <span>{customer.phone}</span>
                      </div>
                      {customer.age && (
                        <div className="flex items-center gap-1.5">
                          <span>{customer.age}</span>
                        </div>
                      )}
                      {customer.gender && (
                        <div className="flex items-center gap-1.5">
                          <span>{getLocalizedGender(customer.gender)}</span>
                        </div>
                      )}
                      {customer.businessType && (
                        <div className="flex items-center gap-1.5">
                          <span>{customer.businessType}</span>
                        </div>
                      )}
                    </div>
                    {customer.certifiedAt && (
                      <div className="flex-shrink-0 text-surface-600 flex items-center gap-1.5">
                        <Calendar size={16} className="text-surface-500" />
                        <span>{format(new Date(customer.certifiedAt), 'MMM dd, yyyy')}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

        </>
      )}
      </div>

      {/* Enhanced Pagination */}
      {pageInfo.totalPages > 1 && (
        <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          {/* Results Info and Page Size Selector */}
          <div className="flex items-center gap-4">
            <div className="text-sm text-gray-700">
              {t('customers.showing')} {Math.min((pageInfo.page - 1) * pageInfo.limit + 1, pageInfo.total)} {t('customers.to')} {Math.min(pageInfo.page * pageInfo.limit, pageInfo.total)} {t('customers.of')} {pageInfo.total} {t('customers.customers')}
            </div>
            <div className="flex items-center gap-2">
              <label htmlFor="pageSize" className="text-sm text-gray-700">
                {t('customers.show')}
              </label>
              <select
                id="pageSize"
                value={pageInfo.limit}
                onChange={(e) => {
                  const newLimit = parseInt(e.target.value);
                  setSearchParams(prev => ({ ...prev, limit: newLimit, page: 1 }));
                }}
                className="text-sm border border-gray-300 rounded-md px-2 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
              <span className="text-sm text-gray-700">{t('customers.perPage')}</span>
            </div>
          </div>

          {/* Pagination Controls */}
          <div className="flex items-center gap-2">
            {/* First Page Button */}
            <button
              onClick={() => handlePageChange(1)}
              disabled={pageInfo.page === 1}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              title={t('approvals.firstPage')}
            >
              <ChevronLeft size={16} />
              <ChevronLeft size={16} className="-ml-1" />
            </button>

            {/* Previous Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.page - 1)}
              disabled={pageInfo.page === 1}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title={t('approvals.previousPage')}
            >
              <ChevronLeft size={16} />
            </button>

            {/* Page Numbers */}
            <div className="flex items-center gap-1">
              {getPageNumbers().map((pageNum, index) => (
                <button
                  key={`${pageNum}-${index}`}
                  onClick={() => typeof pageNum === 'number' ? handlePageChange(pageNum) : undefined}
                  disabled={typeof pageNum !== 'number'}
                  className={`px-3 py-2 text-sm font-medium rounded-md ${
                    pageNum === pageInfo.page
                      ? 'bg-indigo-600 text-white border border-indigo-600'
                      : typeof pageNum === 'number'
                      ? 'text-gray-700 bg-white border border-gray-300 hover:bg-gray-50'
                      : 'text-gray-400 bg-white border border-transparent cursor-default'
                  }`}
                >
                  {pageNum}
                </button>
              ))}
            </div>

            {/* Next Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.page + 1)}
              disabled={pageInfo.page === pageInfo.totalPages}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title={t('approvals.nextPage')}
            >
              <ChevronRight size={16} />
            </button>

            {/* Last Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.totalPages)}
              disabled={pageInfo.page === pageInfo.totalPages}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              title={t('approvals.lastPage')}
            >
              <ChevronRight size={16} />
              <ChevronRight size={16} className="-ml-1" />
            </button>
          </div>
        </div>
      )}
      </div>
      </div>
    </div>
  );
}