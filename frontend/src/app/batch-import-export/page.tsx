'use client';

import { useState } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { customerImportApi, ImportSummary } from '@/lib/api';
import { Upload, FileSpreadsheet, CheckCircle, AlertCircle } from 'lucide-react';
import StagingList from '@/components/batch-import/StagingList';

export default function BatchImportExportPage() {
  const { t } = useLanguage();
  const { user } = useAuth();

  // Import states
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [stagingRefreshTrigger, setStagingRefreshTrigger] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [importResult, setImportResult] = useState<ImportSummary | null>(null);

  const canImport = user?.role === 'ADMIN' || user?.role === 'OFFICER';

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setUploadMessage(null);
    } else {
      setFile(null);
    }
  };

  const handleUpload = async () => {
    if (!file) {
      return;
    }

    try {
      setUploading(true);
      setUploadMessage(null);

      const response = await customerImportApi.uploadFile(file);
      setUploadMessage({ type: 'success', text: response.message });
      setFile(null);

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }

      // Trigger refresh of staging list
      setStagingRefreshTrigger(prev => prev + 1);
    } catch (err) {
      setUploadMessage({ type: 'error', text: t('batchImport.uploadFailed') });
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleConfirmImport = async () => {
    try {
      setConfirming(true);
      const summary = await customerImportApi.confirmImport();
      setImportResult(summary);
      setStagingRefreshTrigger(prev => prev + 1);
    } catch (err) {
      console.error('Failed to confirm import:', err);
    } finally {
      setConfirming(false);
    }
  };

  const handleCancelImport = async () => {
    try {
      await customerImportApi.cancelImport();
      setStagingRefreshTrigger(prev => prev + 1);
      setImportResult(null);
    } catch (err) {
      console.error('Failed to cancel import:', err);
    }
  };

  // Show permission denied for non-admin/officer
  if (!canImport) {
    return (
      <div className="min-h-screen bg-gray-50 p-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <AlertCircle className="w-16 h-16 text-orange-500 mx-auto mb-4" />
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">
              {t('batchImport.permissionDenied')}
            </h2>
            <p className="text-gray-500">
              {t('batchImport.noPermissionMessage')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        <div className="space-y-6">
          {/* File Upload */}
          <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">{t('batchImport.uploadExcel')}</h2>
              <p className="text-gray-600 mb-4">
                {t('batchImport.uploadDescription')}
              </p>

              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {t('batchImport.selectFile')}
                </label>
                <input
                  id="file-upload"
                  type="file"
                  accept=".xlsx"
                  onChange={handleFileSelect}
                  className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                />
              </div>

              {file && (
                <div className="mb-4 p-3 bg-gray-50 rounded-md flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <FileSpreadsheet className="w-5 h-5 text-green-600" />
                    <span className="text-sm font-medium text-gray-700">{file.name}</span>
                    <span className="text-xs text-gray-500">({(file.size / 1024).toFixed(2)} KB)</span>
                  </div>
                  <button
                    onClick={() => {
                      setFile(null);
                      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
                      if (fileInput) {
                        fileInput.value = '';
                      }
                    }}
                    className="text-red-600 hover:text-red-800 text-sm"
                  >
                    {t('batchImport.remove')}
                  </button>
                </div>
              )}

              {uploadMessage && (
                <div className={`mb-4 p-3 rounded-md ${
                  uploadMessage.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                }`}>
                  {uploadMessage.text}
                </div>
              )}

              <button
                onClick={handleUpload}
                disabled={!file || uploading}
                className="btn-primary flex items-center gap-2"
              >
                <Upload size={18} />
                {uploading ? t('batchImport.uploading') : t('batchImport.uploadFile')}
              </button>
            </div>

            {/* Staged Records List with Pagination */}
            <StagingList refreshTrigger={stagingRefreshTrigger} />

            {/* Import Action Buttons */}
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex justify-end gap-3">
                <button
                  onClick={handleCancelImport}
                  className="btn-secondary"
                >
                  {t('batchImport.cancelClear')}
                </button>
                <button
                  onClick={handleConfirmImport}
                  disabled={confirming}
                  className="btn-primary flex items-center gap-2"
                >
                  <CheckCircle size={18} />
                  {confirming ? t('batchImport.importing') : t('batchImport.confirmImport')}
                </button>
              </div>
            </div>

            {/* Import Result */}
            {importResult && (
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">{t('batchImport.importCompleted')}</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                    <div className="text-2xl font-bold text-green-600">{importResult.imported}</div>
                    <div className="text-sm text-green-700">{t('batchImport.newCustomersImported')}</div>
                  </div>
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <div className="text-2xl font-bold text-blue-600">{importResult.updated}</div>
                    <div className="text-sm text-blue-700">{t('batchImport.customersUpdated')}</div>
                  </div>
                  <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                    <div className="text-2xl font-bold text-orange-600">{importResult.skipped}</div>
                    <div className="text-sm text-orange-700">{t('batchImport.skipped')}</div>
                  </div>
                </div>
                <button
                  onClick={() => setImportResult(null)}
                  className="mt-4 btn-secondary"
                >
                  {t('batchImport.importMore')}
                </button>
              </div>
            )}
        </div>
      </div>
    </div>
  );
}
