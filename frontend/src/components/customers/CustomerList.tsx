'use client';

import { useState, useEffect, useCallback } from 'react';
import { Search, Plus, Phone, Calendar, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { Customer, CustomerSearchParams, CustomerPageResponse, CertificateTypeTranslationKeys, CustomerStatusTranslationKeys, CertificateType, CustomerStatus } from '@/types/customer';
import { customerApi } from '@/lib/api';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { format } from 'date-fns';
import { enUS, zhCN } from 'date-fns/locale';
import { SalesRole } from '@/types/auth';

interface CustomerListProps {
  onCustomerSelect?: (customer: Customer) => void;
  onCreateCustomer?: () => void;
}

export default function CustomerList({ onCustomerSelect, onCreateCustomer }: CustomerListProps) {
  const { t, language } = useLanguage();
  const { user } = useAuth();

  // Helper functions for localStorage
  const STORAGE_KEY = 'customerListFilters';

  interface StoredFilters {
    searchTerm?: string;
    certifiedStartDate?: string;
    certifiedEndDate?: string;
    selectedCertificateType?: string;
    selectedStatus?: string;
    certificateIssuer?: string;
    customerAgent?: string;
    page?: number;
    pageSize?: number;
  }

  const loadStoredFilters = (): StoredFilters | null => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (error) {
      console.error('Error loading filters from localStorage:', error);
    }
    return null;
  };

  const saveFiltersToStorage = useCallback((filters: StoredFilters) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
    } catch (error) {
      console.error('Error saving filters to localStorage:', error);
    }
  }, []);

  // Helper function to check if text is a phone number
  const isPhoneNumber = (text: string): boolean => {
    const phoneRegex = /^[\d+s\-()]+$/;
    return phoneRegex.test(text.trim()) && text.replace(/\D/g, '').length >= 3;
  };

  // Initialize state from localStorage or defaults
  const storedFilters = loadStoredFilters();

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initialize filter values from stored filters
  const initialSearchTerm = storedFilters?.searchTerm || '';
  const initialCertifiedStartDate = storedFilters?.certifiedStartDate || '';
  const initialCertifiedEndDate = storedFilters?.certifiedEndDate || '';
  const initialSelectedCertificateType = storedFilters?.selectedCertificateType || '';
  const initialSelectedStatus = storedFilters?.selectedStatus || '';
  const initialCertificateIssuer = storedFilters?.certificateIssuer || '';
  const initialCustomerAgent = storedFilters?.customerAgent || '';
  const initialPageSize = storedFilters?.pageSize || 20;

  // Initialize search params with stored filters
  const isInitialSearchPhone = isPhoneNumber(initialSearchTerm);

  const [searchParams, setSearchParams] = useState<CustomerSearchParams>({
    page: storedFilters?.page || 1,
    limit: initialPageSize,
    q: !isInitialSearchPhone ? initialSearchTerm || undefined : undefined,
    phone: isInitialSearchPhone ? initialSearchTerm || undefined : undefined,
    status: initialSelectedStatus ? initialSelectedStatus as CustomerStatus : undefined,
    certificateType: initialSelectedCertificateType ? initialSelectedCertificateType as CertificateType : undefined,
    certificateIssuer: initialCertificateIssuer.trim() || undefined,
    customerAgent: initialCustomerAgent.trim() || undefined,
    certifiedStartDate: initialCertifiedStartDate ? `${initialCertifiedStartDate}T00:00:00Z` : undefined,
    certifiedEndDate: initialCertifiedEndDate ? `${initialCertifiedEndDate}T00:00:00Z` : undefined,
  });

  const [pageInfo, setPageInfo] = useState({
    total: 0,
    totalPages: 0,
    page: storedFilters?.page || 1,
    limit: initialPageSize,
  });
  const [searchTerm, setSearchTerm] = useState(initialSearchTerm);
  const [certifiedStartDate, setCertifiedStartDate] = useState(initialCertifiedStartDate);
  const [certifiedEndDate, setCertifiedEndDate] = useState(initialCertifiedEndDate);
  const [selectedCertificateType, setSelectedCertificateType] = useState(initialSelectedCertificateType);
  const [selectedStatus, setSelectedStatus] = useState(initialSelectedStatus);
  const [certificateIssuer, setCertificateIssuer] = useState(initialCertificateIssuer);
  const [customerAgent, setCustomerAgent] = useState(initialCustomerAgent);

  // Map language to date-fns locale
  const locale = language === 'zh-CN' ? zhCN : enUS;

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

  // Save all filters to localStorage whenever they change
  useEffect(() => {
    const filters = {
      searchTerm,
      certifiedStartDate,
      certifiedEndDate,
      selectedCertificateType,
      selectedStatus,
      certificateIssuer,
      customerAgent,
      page: searchParams.page,
      pageSize: searchParams.limit,
    };
    saveFiltersToStorage(filters);
  }, [searchTerm, certifiedStartDate, certifiedEndDate, selectedCertificateType, selectedStatus, certificateIssuer, customerAgent, searchParams.page, searchParams.limit, saveFiltersToStorage, t]);

  useEffect(() => {
    loadCustomers(searchParams);
  }, [searchParams, loadCustomers]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedSearchTerm = searchTerm.trim();

    setSearchParams(prev => ({
      ...prev,
      q: !isPhoneNumber(trimmedSearchTerm) ? trimmedSearchTerm || undefined : undefined,
      phone: isPhoneNumber(trimmedSearchTerm) ? trimmedSearchTerm || undefined : undefined,
      status: selectedStatus ? selectedStatus as CustomerStatus : undefined,
      certificateType: selectedCertificateType ? selectedCertificateType as CertificateType : undefined,
      certificateIssuer: certificateIssuer.trim() || undefined,
      customerAgent: customerAgent.trim() || undefined,
      certifiedStartDate: certifiedStartDate ? `${certifiedStartDate}T00:00:00Z` : undefined,
      certifiedEndDate: certifiedEndDate ? `${certifiedEndDate}T00:00:00Z` : undefined,
      page: 1,
    }));
  };

  const handleClearAllFilters = () => {
    setSearchTerm('');
    setSelectedStatus('');
    setSelectedCertificateType('');
    setCertificateIssuer('');
    setCustomerAgent('');
    setCertifiedStartDate('');
    setCertifiedEndDate('');
    setSearchParams({
      page: 1,
      limit: searchParams.limit,
    });
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
    <div className="flex flex-col h-full">
      {/* Fixed Top Bar: Search & Filters */}
      <div className="px-8 py-4 flex-shrink-0">
        <form onSubmit={handleSearch}>
          {/* Row 1: Search Input + Status + Certificate Type + Clear Button */}
          <div className="flex gap-3 mb-3 items-end">
            <div className="grid grid-cols-12 gap-3 flex-1">
              {/* Search Input - 5 columns (same as certificate issuer) */}
              <div className="col-span-5">
                <label className="block text-xs text-gray-600 mb-1">{t('customers.name')} / {t('customers.phone')}</label>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-surface-400" size={18} />
                  <input
                    type="text"
                    placeholder={t('customers.searchPlaceholder')}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="input-field w-full pl-10"
                  />
                </div>
              </div>

              {/* Status Filter - 2 columns */}
              <div className="col-span-2">
                <label className="block text-xs text-gray-600 mb-1">{t('customers.searchStatus')}</label>
                <select
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                  className="input-field w-full text-sm"
                >
                  <option value="">{t('customers.all')}</option>
                  {Object.values(CustomerStatus).map((status) => (
                    <option key={status} value={status}>
                      {t(CustomerStatusTranslationKeys[status])}
                    </option>
                  ))}
                </select>
              </div>

              {/* Certificate Type Filter - 2 columns */}
              <div className="col-span-2">
                <label className="block text-xs text-gray-600 mb-1">{t('customers.form.certificateType')}</label>
                <select
                  value={selectedCertificateType}
                  onChange={(e) => setSelectedCertificateType(e.target.value)}
                  className="input-field w-full text-sm"
                >
                  <option value="">{t('customers.all')}</option>
                  {Object.values(CertificateType).map((type) => (
                    <option key={type} value={type}>
                      {t(CertificateTypeTranslationKeys[type])}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Clear Button - at the end of row 1 - wider */}
            <button
              type="button"
              onClick={handleClearAllFilters}
              className="btn-secondary flex items-center justify-center gap-2 px-8 py-2 flex-shrink-0 w-[140px]"
            >
              <X size={16} />
              <span>{t('customers.clear')}</span>
            </button>
          </div>

          {/* Row 2: Certificate Issuer + Customer Agent + Start Date + End Date + Search Button */}
          <div className="flex gap-3 items-end">
            <div className="grid grid-cols-12 gap-3 flex-1">
              {/* Certificate Issuer - 5 columns (expanded) */}
              <div className="col-span-5">
                <label className="block text-xs text-gray-600 mb-1">{t('customers.form.certificateIssuer')}</label>
                <input
                  type="text"
                  value={certificateIssuer}
                  onChange={(e) => setCertificateIssuer(e.target.value)}
                  placeholder={t('customers.form.certificateIssuer.placeholder')}
                  className="input-field w-full text-sm"
                />
              </div>

              {/* Customer Agent - 2 columns (conditional for Admin/Officer) */}
              {(user?.role === SalesRole.ADMIN || user?.role === SalesRole.OFFICER) && (
                <div className="col-span-2">
                  <label className="block text-xs text-gray-600 mb-1">{t('customers.form.customerAgent')}</label>
                  <input
                    type="text"
                    value={customerAgent}
                    onChange={(e) => setCustomerAgent(e.target.value)}
                    placeholder={t('customers.form.customerAgent.placeholder')}
                    className="input-field w-full text-sm"
                  />
                </div>
              )}

              {/* Start Date - 2 columns */}
              <div className="col-span-2">
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs text-gray-600">{t('customers.startDate')}</label>
                  {certifiedStartDate && (
                    <button
                      type="button"
                      onClick={() => setCertifiedStartDate('')}
                      className="text-gray-400 hover:text-gray-600 transition-colors"
                      aria-label="Clear start date"
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>
                <input
                  type="date"
                  value={certifiedStartDate}
                  onChange={(e) => setCertifiedStartDate(e.target.value)}
                  className="input-field w-full text-sm"
                />
              </div>

              {/* End Date - 2 columns */}
              <div className="col-span-2">
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs text-gray-600">{t('customers.endDate')}</label>
                  {certifiedEndDate && (
                    <button
                      type="button"
                      onClick={() => setCertifiedEndDate('')}
                      className="text-gray-400 hover:text-gray-600 transition-colors"
                      aria-label="Clear end date"
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>
                <input
                  type="date"
                  value={certifiedEndDate}
                  onChange={(e) => setCertifiedEndDate(e.target.value)}
                  className="input-field w-full text-sm"
                />
              </div>
            </div>

            {/* Search Button - at the end of row 2 - wider */}
            <button
              type="submit"
              className="btn-primary flex items-center justify-center gap-2 px-8 py-2 flex-shrink-0 w-[140px]"
            >
              <Search size={16} />
              <span>{t('customers.search')}</span>
            </button>
          </div>
        </form>
      </div>

      {/* Floating Add Customer Button - Above Pagination at Bottom Right */}
      {(user?.role === SalesRole.ADMIN || user?.role === SalesRole.OFFICER) && (
        <button
          onClick={onCreateCustomer}
          className="fixed bottom-24 right-8 w-14 h-14 bg-primary-500 hover:bg-primary-600 text-white rounded-full shadow-lg flex items-center justify-center transition-all duration-200 hover:scale-110 hover:shadow-xl z-50"
          aria-label={t('customers.addCustomer')}
        >
          <Plus size={24} strokeWidth={2.5} />
        </button>
      )}

      {/* Scrollable Customer List */}
      <div className="flex-1 min-h-0 overflow-hidden flex flex-col px-8">
        {loading ? (
          <div className="card-elevated">
            <div className="card-content text-center py-12">
              <div className="loading-skeleton w-16 h-16 rounded-full mx-auto mb-4"></div>
              <div className="text-body-1 text-surface-600">{t('customers.loadingCustomers')}</div>
            </div>
          </div>
        ) : customers.length === 0 ? (
          <div className="card-elevated">
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
                    setSearchParams(prev => ({ ...prev, q: undefined, phone: undefined, page: 1 }));
                  }}
                  className="btn-outline"
                >
                  {t('customers.clearSearch')}
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="overflow-hidden shadow ring-1 ring-black ring-opacity-5 md:rounded-lg flex flex-col h-full">
            <div className="overflow-y-auto flex-1">
              <table className="min-w-full divide-y divide-gray-300">
                <thead className="bg-gray-50 sticky top-0 z-10">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.name')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.phone')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.form.age')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.form.gender')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.form.certificateType')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.form.certificateIssuer')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customers.form.certifiedAt')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customer.salesPerson')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('customer.currentStatus')}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {customers.map((customer) => (
                    <tr
                      key={customer.id}
                      onClick={() => handleCustomerClick(customer)}
                      className="hover:bg-gray-50 cursor-pointer"
                    >
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{customer.name}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <div className="flex items-center gap-1.5">
                          <Phone size={14} className="text-gray-400" />
                          <span>{customer.phone}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {customer.age || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {customer.gender ? getLocalizedGender(customer.gender) : '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {customer.certificateType ? t(CertificateTypeTranslationKeys[customer.certificateType]) : '-'}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">
                        {customer.certificateIssuer || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {customer.certifiedAt ? (
                          <div className="flex items-center gap-1.5">
                            <Calendar size={14} className="text-gray-400" />
                            <span>{format(new Date(customer.certifiedAt), 'PPP', { locale })}</span>
                          </div>
                        ) : '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {customer.customerAgent || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          customer.currentStatus === 'CERTIFIED'
                            ? 'bg-green-100 text-green-800'
                            : customer.currentStatus === 'SUBMITTED'
                            ? 'bg-blue-100 text-blue-800'
                            : customer.currentStatus === 'NOTIFIED'
                            ? 'bg-yellow-100 text-yellow-800'
                            : customer.currentStatus === 'ABORTED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}>
                          {customer.currentStatus ? t(CustomerStatusTranslationKeys[customer.currentStatus]) : '-'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Fixed Pagination at Bottom */}
      {!loading && customers.length > 0 && (
        <div className="px-8 py-4 flex-shrink-0">
          {/* Results Info and Page Size Selector */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 w-full">
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
        </div>
      )}
    </div>
  );
}