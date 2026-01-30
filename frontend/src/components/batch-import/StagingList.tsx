'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Phone, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { CustomerStaging, StagingPageResponse } from '@/lib/api';
import { CertificateType, CertificateIssuer, CertificateIssuerTranslationKeys, CustomerType, EducationLevel, getTranslatedEducationLevelName, getTranslatedCustomerTypeName } from '@/types/customer';
import { customerImportApi } from '@/lib/api';
import { getCertificateTypeDisplayName } from '@/lib/certificateTypeUtils';
import { useLanguage } from '@/contexts/LanguageContext';
import { format } from 'date-fns';
import { enUS, zhCN } from 'date-fns/locale';

interface StagingListProps {
  refreshTrigger: number;
  onStatsUpdate?: (stats: { valid: number; update: number; duplicate: number; invalid: number }) => void;
  importStatusFilter?: string | null;
}

export default function StagingList({ refreshTrigger, onStatsUpdate, importStatusFilter }: StagingListProps) {
  const { t, language } = useLanguage();

  const [records, setRecords] = useState<CustomerStaging[]>([]);
  const [loading, setLoading] = useState(true);
  const [_stats, setStats] = useState({
    valid: 0,
    update: 0,
    duplicate: 0,
    invalid: 0,
  });
  const [pageInfo, setPageInfo] = useState({
    total: 0,
    totalPages: 0,
    page: 1,
    limit: 100,
  });

  // Track previous filter to detect changes
  const prevImportStatusFilter = useRef<string | null>(importStatusFilter ?? null);

  // Use ref to store current limit to avoid dependency issues
  const limitRef = useRef(pageInfo.limit);
  limitRef.current = pageInfo.limit;

  const locale = language === 'zh-CN' ? zhCN : enUS;

  const loadRecords = useCallback(async (page: number = 1, limit: number = 100) => {
    try {
      setLoading(true);
      const response: StagingPageResponse = await customerImportApi.getStagedRecords(page, limit, importStatusFilter || undefined);
      setRecords(response.items);

      // Calculate statistics (only for displayed records)
      const valid = response.items.filter(r => r.importStatus === 'VALID').length;
      const update = response.items.filter(r => r.importStatus === 'UPDATE').length;
      const duplicate = response.items.filter(r => r.importStatus === 'DUPLICATE').length;
      const invalid = response.items.filter(r => r.importStatus === 'INVALID').length;

      const newStats = { valid, update, duplicate, invalid };
      setStats(newStats);

      // Send stats to parent component
      if (onStatsUpdate) {
        onStatsUpdate(newStats);
      }

      setPageInfo({
        total: response.total,
        totalPages: response.totalPages,
        page: response.page,
        limit: response.limit,
      });
    } catch (err) {
      console.error('Failed to load staged records:', err);
    } finally {
      setLoading(false);
    }
  }, [importStatusFilter, onStatsUpdate]);

  // Combined effect to handle filter changes and refresh
  useEffect(() => {
    const filterChanged = prevImportStatusFilter.current !== (importStatusFilter ?? null);

    if (filterChanged) {
      // Reset to page 1 when filter changes
      prevImportStatusFilter.current = importStatusFilter ?? null;
      loadRecords(1, pageInfo.limit);
      setPageInfo(prev => ({ ...prev, page: 1 }));
    } else {
      // Normal reload with current page
      loadRecords(pageInfo.page, pageInfo.limit);
    }
  }, [refreshTrigger, importStatusFilter]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= pageInfo.totalPages) {
      loadRecords(newPage, pageInfo.limit);
    }
  };

  const handlePageSizeChange = (newLimit: number) => {
    loadRecords(1, newLimit);
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

  const getLocalizedStatus = (status: string): string => {
    const statusMap: Record<string, string> = {
      'VALID': t('batchImport.status.valid'),
      'UPDATE': t('batchImport.status.update'),
      'DUPLICATE': t('batchImport.status.duplicate'),
      'INVALID': t('batchImport.status.invalid'),
    };
    return statusMap[status] || status;
  };

  const hasFieldChanged = (record: CustomerStaging, fieldName: string): boolean => {
    // Only highlight when filtering by UPDATE status and the record is UPDATE
    if (importStatusFilter !== 'UPDATE' || record.importStatus !== 'UPDATE') {
      return false;
    }

    if (!record.changedFields) {
      return false;
    }

    const changedFieldsArray = record.changedFields.split(',');
    return changedFieldsArray.includes(fieldName);
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'VALID':
        return <span className="text-green-600">✓</span>;
      case 'UPDATE':
        return <span className="text-blue-600">↻</span>;
      case 'DUPLICATE':
        return <span className="text-orange-600">!</span>;
      case 'INVALID':
        return <span className="text-red-600">✗</span>;
      default:
        return <span className="text-gray-400">?</span>;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'VALID':
        return 'bg-green-100 text-green-800';
      case 'UPDATE':
        return 'bg-blue-100 text-blue-800';
      case 'DUPLICATE':
        return 'bg-orange-100 text-orange-800';
      case 'INVALID':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="text-center py-12">
          <div className="text-gray-500">{t('customers.loadingCustomers')}</div>
        </div>
      </div>
    );
  }

  if (records.length === 0) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow flex flex-col overflow-hidden border" style={{ maxHeight: '70vh' }}>
      {/* Info banner for UPDATE filter */}
      {importStatusFilter === 'UPDATE' && (
        <div className="bg-blue-50 border-l-4 border-blue-400 px-4 py-2">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-blue-700">
                {t('batchImport.updateFilterInfo')}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Scrollable Table Area */}
      <div className="flex-1 overflow-auto">
        <table className="divide-y divide-gray-300" style={{ minWidth: importStatusFilter === 'INVALID' ? '1000px' : '1400px' }}>
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32 whitespace-nowrap">{t('customers.name')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32 whitespace-nowrap">{t('customers.phone')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-40 whitespace-nowrap">{t('customers.form.idCard')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-36 whitespace-nowrap">{t('customers.form.certifiedAt')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-56 whitespace-nowrap">{t('customers.form.certificateIssuer')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32 whitespace-nowrap">{t('customers.form.certificateType')}</th>
              {importStatusFilter !== 'INVALID' && (
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-36 whitespace-nowrap">{t('customers.form.customerType')}</th>
              )}
              {importStatusFilter !== 'INVALID' && (
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-44 whitespace-nowrap">{t('customers.form.education')}</th>
              )}
              {importStatusFilter !== 'INVALID' && (
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-36 whitespace-nowrap">{t('customer.salesPerson')}</th>
              )}
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-28 whitespace-nowrap">{t('batchImport.status')}</th>
              {importStatusFilter === 'INVALID' && (
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-48 whitespace-nowrap">{t('batchImport.validationMessage')}</th>
              )}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {records.map((record) => (
              <tr key={record.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className={`text-sm font-medium ${
                    hasFieldChanged(record, 'name') ? 'text-red-600' : 'text-gray-900'
                  }`}>{record.name}</div>
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm w-32 ${
                  hasFieldChanged(record, 'phone') ? 'text-red-600' : 'text-gray-500'
                }`}>
                  <div className="flex items-center gap-1.5">
                    <Phone size={14} className="text-gray-400" />
                    <span>{record.phone}</span>
                  </div>
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm w-40 ${
                  hasFieldChanged(record, 'idCard') ? 'text-red-600' : 'text-gray-500'
                }`}>
                  {record.idCard || '-'}
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm w-36 ${
                  hasFieldChanged(record, 'certifiedAt') ? 'text-red-600' : 'text-gray-500'
                }`}>
                  {record.certifiedAt ? (
                    <div className="flex items-center gap-1.5">
                      <Calendar size={14} className="text-gray-400" />
                      <span>{format(new Date(record.certifiedAt), 'PPP', { locale })}</span>
                    </div>
                  ) : '-'}
                </td>
                <td className={`px-6 py-4 text-sm w-56 whitespace-nowrap ${
                  hasFieldChanged(record, 'certificateIssuer') ? 'text-red-600' : 'text-gray-500'
                }`}>
                  {record.certificateIssuer ? t(CertificateIssuerTranslationKeys[record.certificateIssuer as CertificateIssuer]) : '-'}
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm w-32 ${
                  hasFieldChanged(record, 'certificateType') ? 'text-red-600' : 'text-gray-500'
                }`}>
                  {record.certificateType ? getCertificateTypeDisplayName(record.certificateType as CertificateType, t) : '-'}
                </td>
                {importStatusFilter !== 'INVALID' && (
                  <td className={`px-6 py-4 whitespace-nowrap text-sm w-36 ${
                    hasFieldChanged(record, 'customerType') ? 'text-red-600' : 'text-gray-500'
                  }`}>
                    {record.customerType ? getTranslatedCustomerTypeName(record.customerType as CustomerType, t) : '-'}
                  </td>
                )}
                {importStatusFilter !== 'INVALID' && (
                  <td className={`px-6 py-4 whitespace-nowrap text-sm w-44 ${
                    hasFieldChanged(record, 'education') ? 'text-red-600' : 'text-gray-500'
                  }`}>
                    {record.education ? getTranslatedEducationLevelName(record.education as EducationLevel, t) : '-'}
                  </td>
                )}
                {importStatusFilter !== 'INVALID' && (
                  <td className={`px-6 py-4 whitespace-nowrap text-sm w-36 ${
                    hasFieldChanged(record, 'customerAgent') ? 'text-red-600' : 'text-gray-500'
                  }`}>
                    {record.customerAgent || '-'}
                  </td>
                )}
                <td className="px-6 py-4 whitespace-nowrap w-28">
                  <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(record.importStatus)}`}>
                    {getStatusIcon(record.importStatus)}
                    {getLocalizedStatus(record.importStatus)}
                  </span>
                </td>
                {importStatusFilter === 'INVALID' && (
                  <td className="px-6 py-4 text-sm w-48 text-red-600 whitespace-nowrap">
                    {record.validationMessage || '-'}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="border-t border-gray-200 px-1 sm:px-3 py-2 flex-shrink-0">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 w-full">
          <div className="flex flex-col sm:flex-row items-center gap-2 sm:gap-4 text-center sm:text-left">
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
                onChange={(e) => handlePageSizeChange(parseInt(e.target.value))}
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
          <div className="flex items-center justify-center gap-1 sm:gap-2 overflow-x-auto">
            {/* First Page Button */}
            <button
              onClick={() => handlePageChange(1)}
              disabled={pageInfo.page === 1 || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center flex-shrink-0"
              title={t('approvals.firstPage')}
            >
              <ChevronLeft size={16} />
              <ChevronLeft size={16} className="-ml-1" />
            </button>

            {/* Previous Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.page - 1)}
              disabled={pageInfo.page === 1 || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
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
                  disabled={typeof pageNum !== 'number' || loading}
                  className={`px-2 sm:px-3 py-2 text-sm font-medium rounded-md flex-shrink-0 ${
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
              disabled={pageInfo.page === pageInfo.totalPages || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
              title={t('approvals.nextPage')}
            >
              <ChevronRight size={16} />
            </button>

            {/* Last Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.totalPages)}
              disabled={pageInfo.page === pageInfo.totalPages || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center flex-shrink-0"
              title={t('approvals.lastPage')}
            >
              <ChevronRight size={16} />
              <ChevronRight size={16} className="-ml-1" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
