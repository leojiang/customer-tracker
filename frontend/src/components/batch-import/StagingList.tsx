'use client';

import { useState, useEffect } from 'react';
import { Phone, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { CustomerStaging, StagingPageResponse } from '@/lib/api';
import { CertificateType, CertificateIssuer, CertificateIssuerTranslationKeys, EducationLevel, getTranslatedEducationLevelName } from '@/types/customer';
import { customerImportApi } from '@/lib/api';
import { getCertificateTypeDisplayName } from '@/lib/certificateTypeUtils';
import { useLanguage } from '@/contexts/LanguageContext';
import { format } from 'date-fns';
import { enUS, zhCN } from 'date-fns/locale';

interface StagingListProps {
  refreshTrigger: number;
  onStatsUpdate?: (stats: { valid: number; update: number; duplicate: number; invalid: number }) => void;
}

export default function StagingList({ refreshTrigger, onStatsUpdate }: StagingListProps) {
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
    limit: 20,
  });

  const locale = language === 'zh-CN' ? zhCN : enUS;

  const loadRecords = async (page: number = 1, limit: number = 20) => {
    try {
      setLoading(true);
      const response: StagingPageResponse = await customerImportApi.getStagedRecords(page, limit);
      setRecords(response.items);

      // Calculate statistics
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
  };

  useEffect(() => {
    loadRecords(pageInfo.page, pageInfo.limit);
  }, [refreshTrigger]); // eslint-disable-line react-hooks/exhaustive-deps

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

  const getLocalizedGender = (gender: string): string => {
    if (gender === '男' || gender.toLowerCase() === 'male') {
      return t('customers.form.gender.male');
    }
    if (gender === '女' || gender.toLowerCase() === 'female') {
      return t('customers.form.gender.female');
    }
    return gender;
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
    <div className="bg-white rounded-lg shadow flex flex-col" style={{ maxHeight: '70vh' }}>
      {/* Scrollable Table Area */}
      <div className="flex-1 overflow-auto">
        <table className="divide-y divide-gray-300" style={{ minWidth: '1400px' }}>
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32">{t('customers.name')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-28">{t('customers.form.gender')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32">{t('customers.phone')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-40">{t('customers.form.idCard')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-36">{t('customers.form.certifiedAt')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-56">{t('customers.form.certificateIssuer')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-32">{t('customers.form.certificateType')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-44">{t('customers.form.education')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-36">{t('customer.salesPerson')}</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider sticky top-0 left-0 z-10 bg-gray-50 w-28">{t('batchImport.status')}</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {records.map((record) => (
              <tr key={record.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{record.name}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-28">
                  {record.gender ? getLocalizedGender(record.gender) : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-32">
                  <div className="flex items-center gap-1.5">
                    <Phone size={14} className="text-gray-400" />
                    <span>{record.phone}</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-40">
                  {record.idCard || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-36">
                  {record.certifiedAt ? (
                    <div className="flex items-center gap-1.5">
                      <Calendar size={14} className="text-gray-400" />
                      <span>{format(new Date(record.certifiedAt), 'PPP', { locale })}</span>
                    </div>
                  ) : '-'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-500 w-56">
                  {record.certificateIssuer ? t(CertificateIssuerTranslationKeys[record.certificateIssuer as CertificateIssuer]) : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-32">
                  {record.certificateType ? getCertificateTypeDisplayName(record.certificateType as CertificateType, t) : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-44">
                  {record.education ? getTranslatedEducationLevelName(record.education as EducationLevel, t) : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-36">
                  {record.customerAgent || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap w-28">
                  <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(record.importStatus)}`}>
                    {getStatusIcon(record.importStatus)}
                    {record.importStatus}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="border-t border-gray-200 px-8 py-4 flex-shrink-0">
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
          <div className="flex items-center gap-2">
            {/* First Page Button */}
            <button
              onClick={() => handlePageChange(1)}
              disabled={pageInfo.page === 1 || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              title={t('approvals.firstPage')}
            >
              <ChevronLeft size={16} />
              <ChevronLeft size={16} className="-ml-1" />
            </button>

            {/* Previous Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.page - 1)}
              disabled={pageInfo.page === 1 || loading}
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
                  disabled={typeof pageNum !== 'number' || loading}
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
              disabled={pageInfo.page === pageInfo.totalPages || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title={t('approvals.nextPage')}
            >
              <ChevronRight size={16} />
            </button>

            {/* Last Page Button */}
            <button
              onClick={() => handlePageChange(pageInfo.totalPages)}
              disabled={pageInfo.page === pageInfo.totalPages || loading}
              className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
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
