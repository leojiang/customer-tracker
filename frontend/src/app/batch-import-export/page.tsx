'use client';

import { useState, useEffect } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { customerImportApi, ImportSummary, StagingStatistics } from '@/lib/api';
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
  const [stagingStats, setStagingStats] = useState<StagingStatistics>({
    valid: 0,
    update: 0,
    duplicate: 0,
    invalid: 0,
  });

  const canImport = user?.role === 'ADMIN' || user?.role === 'OFFICER';

  const loadStagingStatistics = async () => {
    try {
      const stats = await customerImportApi.getStagingStatistics();
      setStagingStats(stats);
    } catch (err) {
      console.error('Failed to load staging statistics:', err);
    }
  };

  // Load statistics on mount and when staging refreshes
  useEffect(() => {
    loadStagingStatistics();
  }, [stagingRefreshTrigger]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setUploadMessage(null);
      setImportResult(null); // Clear import result when selecting a new file
      setStagingStats({ valid: 0, update: 0, duplicate: 0, invalid: 0 }); // Clear statistics
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

  const handleStatsUpdate = (stats: { valid: number; update: number; duplicate: number; invalid: number }) => {
    // No longer needed - we use overall statistics from the API
    // Keeping this for compatibility with StagingList component
  };

  const handleConfirmImport = async () => {
    try {
      setConfirming(true);
      const summary = await customerImportApi.confirmImport();

      // Only set import result if there was actual activity
      if (summary.imported > 0 || summary.updated > 0 || summary.skipped > 0) {
        setImportResult(summary);
      } else {
        setImportResult(null);
      }

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
      setUploadMessage(null); // Clear upload message (success or error)
      setFile(null); // Clear selected file
      setStagingStats({ valid: 0, update: 0, duplicate: 0, invalid: 0 }); // Clear statistics

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
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
    <div className="h-screen bg-gray-50 flex flex-col overflow-hidden">
      <div className="flex-1 overflow-y-auto p-4">
        <div className="max-w-7xl mx-auto space-y-4">
          {/* Top Component */}
          <div className="bg-white rounded-lg shadow p-4">
            {/* Header and Action Buttons */}
            <div className="flex items-center justify-between gap-4 mb-4">
              <h1 className="text-xl font-semibold text-gray-900">{t('batchImport.uploadExcel')}</h1>

              <div className="flex items-center gap-3">
                <input
                  id="file-upload"
                  type="file"
                  accept=".xlsx"
                  onChange={handleFileSelect}
                  className="hidden"
                />

                <button
                  onClick={() => document.getElementById('file-upload')?.click()}
                  className="btn-secondary focus:outline-none focus:ring-0 active:outline-none active:ring-0"
                >
                  {t('batchImport.selectFile')}
                </button>

                {/* Action Buttons */}
                <button
                  onClick={handleCancelImport}
                  className="btn-secondary focus:outline-none focus:ring-0 active:outline-none active:ring-0"
                >
                  {t('batchImport.cancelClear')}
                </button>
                <button
                  onClick={handleConfirmImport}
                  disabled={confirming}
                  className="btn-primary focus:outline-none focus:ring-0 active:outline-none active:ring-0 flex items-center gap-2"
                >
                  <CheckCircle size={18} />
                  {confirming ? t('batchImport.importing') : t('batchImport.confirmImport')}
                </button>
              </div>
            </div>

            {/* Second Row - File Info and Actions */}
            {file && (
              <div className="flex items-center justify-between gap-4 p-4 bg-gray-50 rounded-lg mb-4">
                <div className="flex items-center gap-3">
                  <FileSpreadsheet className="w-5 h-5 text-green-600" />
                  <span className="text-sm font-medium text-gray-700">{file.name}</span>
                  <span className="text-xs text-gray-500">({(file.size / 1024).toFixed(2)} KB)</span>
                </div>

                <div className="flex items-center gap-3">
                  <button
                    onClick={() => {
                      setFile(null);
                      setUploadMessage(null); // Clear upload message (error or success)
                      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
                      if (fileInput) {
                        fileInput.value = '';
                      }
                    }}
                    className="text-red-600 hover:text-red-800 text-sm font-medium"
                    title={t('batchImport.remove')}
                  >
                    {t('batchImport.remove')}
                  </button>

                  <button
                    onClick={handleUpload}
                    disabled={uploading}
                    className="text-indigo-600 hover:text-indigo-800 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                  >
                    <Upload size={16} />
                    {uploading ? t('batchImport.uploading') : t('batchImport.uploadNow')}
                  </button>
                </div>
              </div>
            )}

            {uploadMessage && (
              <div className={`mb-4 p-3 rounded-md text-sm ${
                uploadMessage.type === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
              }`}>
                {uploadMessage.text}
              </div>
            )}

            {/* Statistics Section */}
            {importResult ? (
              /* Import Result Statistics (after confirmed import) */
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
            ) : stagingStats.valid + stagingStats.update + stagingStats.duplicate + stagingStats.invalid > 0 ? (
              /* Staging Statistics (after file upload) */
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <div className="text-2xl font-bold text-green-600">{stagingStats.valid}</div>
                  <div className="text-sm text-green-700">{t('batchImport.newToImport')}</div>
                </div>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <div className="text-2xl font-bold text-blue-600">{stagingStats.update}</div>
                  <div className="text-sm text-blue-700">{t('batchImport.toUpdate')}</div>
                </div>
                <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                  <div className="text-2xl font-bold text-orange-600">{stagingStats.duplicate}</div>
                  <div className="text-sm text-orange-700">{t('batchImport.duplicates')}</div>
                </div>
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="text-2xl font-bold text-red-600">{stagingStats.invalid}</div>
                  <div className="text-sm text-red-700">{t('batchImport.invalid')}</div>
                </div>
              </div>
            ) : null}
          </div>

          {/* Staging List */}
          <StagingList
            refreshTrigger={stagingRefreshTrigger}
            onStatsUpdate={handleStatsUpdate}
          />
        </div>
      </div>
    </div>
  );
}
