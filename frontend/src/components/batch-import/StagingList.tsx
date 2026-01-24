'use client';

import { useState, useEffect } from 'react';
import { Phone, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import { CustomerStaging, StagingPageResponse } from '@/lib/api';
import { CertificateType, EducationLevel, getTranslatedEducationLevelName } from '@/types/customer';
import { customerImportApi } from '@/lib/api';
import { getCertificateTypeDisplayName } from '@/lib/certificateTypeUtils';
import { useLanguage } from '@/contexts/LanguageContext';
import { format } from 'date-fns';
import { enUS, zhCN } from 'date-fns/locale';

interface StagingListProps {
  refreshTrigger: number;
}

export default function StagingList({ refreshTrigger }: StagingListProps) {
  const { t, language } = useLanguage();

  const [records, setRecords] = useState<CustomerStaging[]>([]);
  const [loading, setLoading] = useState(true);
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
    if (newPage >= 1) {
      if (newPage <= pageInfo.totalPages) {
        loadRecords(newPage, pageInfo.limit);
      }
    }
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
    <div className="bg-white rounded-lg shadow">
      <div className="flex items-center justify-between p-6 border-b border-gray-200">
        <h2 className="text-xl font-semibold text-gray-900">
          {t('batchImport.stagedRecords')} ({pageInfo.total} {t('batchImport.totalRecords')})
        </h2>
      </div>

      <div className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-300">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('batchImport.row')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.name')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.age')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.gender')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.phone')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.idCard')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.certifiedAt')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.certificateIssuer')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.certificateType')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customers.form.education')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('customer.salesPerson')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('batchImport.status')}</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {records.map((record) => (
                <tr key={record.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {record.rowNumber}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{record.name}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.age || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.gender ? getLocalizedGender(record.gender) : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex items-center gap-1.5">
                      <Phone size={14} className="text-gray-400" />
                      <span>{record.phone}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.idCard || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.certifiedAt ? (
                      <div className="flex items-center gap-1.5">
                        <Calendar size={14} className="text-gray-400" />
                        <span>{format(new Date(record.certifiedAt), 'PPP', { locale })}</span>
                      </div>
                    ) : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">
                    {record.certificateIssuer || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.certificateType ? getCertificateTypeDisplayName(record.certificateType as CertificateType, t) : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.education ? getTranslatedEducationLevelName(record.education as EducationLevel, t) : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {record.customerAgent || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
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
      </div>

      {/* Pagination */}
      <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="text-sm text-gray-700">
            {t('batchImport.showing')} {Math.min((pageInfo.page - 1) * pageInfo.limit + 1, pageInfo.total)} {t('batchImport.to')} {Math.min(pageInfo.page * pageInfo.limit, pageInfo.total)} {t('batchImport.of')} {pageInfo.total} {t('batchImport.records')}
          </div>
          <div className="flex items-center gap-2">
            <label htmlFor="pageSize" className="text-sm text-gray-700">{t('batchImport.show')}</label>
            <select
              id="pageSize"
              value={pageInfo.limit}
              onChange={(e) => {
                const newLimit = parseInt(e.target.value);
                loadRecords(1, newLimit);
              }}
              className="text-sm border border-gray-300 rounded-md px-2 py-1 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
            <span className="text-sm text-gray-700">{t('batchImport.perPage')}</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => handlePageChange(1)}
            disabled={pageInfo.page === 1}
            className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            title={t('batchImport.firstPage')}
          >
            <ChevronLeft size={16} />
            <ChevronLeft size={16} className="-ml-1" />
          </button>
          <button
            onClick={() => handlePageChange(pageInfo.page - 1)}
            disabled={pageInfo.page === 1}
            className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronLeft size={16} />
          </button>

          <span className="px-3 py-2 text-sm text-gray-700">
            {t('batchImport.page')} {pageInfo.page} {t('batchImport.ofPages')} {pageInfo.totalPages}
          </span>

          <button
            onClick={() => handlePageChange(pageInfo.page + 1)}
            disabled={pageInfo.page === pageInfo.totalPages}
            className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronRight size={16} />
          </button>
          <button
            onClick={() => handlePageChange(pageInfo.totalPages)}
            disabled={pageInfo.page === pageInfo.totalPages}
            className="px-2 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            title={t('batchImport.lastPage')}
          >
            <ChevronRight size={16} />
            <ChevronRight size={16} className="-ml-1" />
          </button>
        </div>
      </div>
    </div>
  );
}
