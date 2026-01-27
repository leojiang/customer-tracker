'use client';

import {useState, useEffect} from 'react';
import {useLanguage} from '@/contexts/LanguageContext';
import {useAuth} from '@/contexts/AuthContext';
import {customerImportApi, ImportSummary, StagingStatistics} from '@/lib/api';
import {Upload, FileSpreadsheet, CheckCircle, AlertCircle, Loader2} from 'lucide-react';
import StagingList from '@/components/batch-import/StagingList';

export default function BatchImportExportPage() {
  const {t} = useLanguage();
  const {user} = useAuth();

  // Import states
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [stagingRefreshTrigger, setStagingRefreshTrigger] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [importResult, setImportResult] = useState<ImportSummary | null>(null);
  const [stagingStats, setStagingStats] = useState<StagingStatistics>({
    valid: 0,
    update: 0,
    duplicate: 0,
    invalid: 0,
  });
  const [selectedStatus, setSelectedStatus] = useState<string | null>(null);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);

  const canImport = user?.role === 'ADMIN' || user?.role === 'OFFICER';

  const hasStagedData = () => {
    return stagingStats.valid + stagingStats.update + stagingStats.duplicate + stagingStats.invalid > 0;
  };

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
      const selectedFile = e.target.files[0];

      // Check if there's existing staged data
      if (hasStagedData()) {
        // Store the pending file and show confirmation dialog
        setPendingFile(selectedFile);
        setShowConfirmDialog(true);

        // Reset the file input so the same file can be selected again if needed
        e.target.value = '';
      } else {
        // No staged data, proceed normally
        setFile(selectedFile);
        setUploadMessage(null);
        setImportResult(null); // Clear import result when selecting a new file
        setStagingStats({valid: 0, update: 0, duplicate: 0, invalid: 0}); // Clear statistics
        setSelectedStatus(null); // Reset filter
      }
    } else {
      setFile(null);
    }
  };

  const handleStatusFilter = (status: string | null) => {
    // Toggle the filter: if clicking the same status, clear it
    if (selectedStatus === status) {
      setSelectedStatus(null);
    } else {
      setSelectedStatus(status);
    }
  };

  const handleConfirmDialogYes = async () => {
    // User confirmed: cancel existing import and proceed with new file
    try {
      setCancelling(true);
      await customerImportApi.cancelImport();
      setStagingRefreshTrigger(prev => prev + 1);
      setImportResult(null);
      setUploadMessage(null);
      setStagingStats({valid: 0, update: 0, duplicate: 0, invalid: 0});
      setSelectedStatus(null);

      // Set the new file
      setFile(pendingFile);
      setUploadMessage(null);
      setImportResult(null);
      setStagingStats({valid: 0, update: 0, duplicate: 0, invalid: 0});
      setSelectedStatus(null);

      // Clear pending file and close dialog
      setPendingFile(null);
      setShowConfirmDialog(false);
    } catch (err) {
      console.error('Failed to cancel import:', err);
      setPendingFile(null);
      setShowConfirmDialog(false);
    } finally {
      setCancelling(false);
    }
  };

  const handleConfirmDialogNo = () => {
    // User cancelled: just close the dialog and keep existing data
    setPendingFile(null);
    setShowConfirmDialog(false);
  };

  const handleUpload = async () => {
    if (!file) {
      return;
    }

    try {
      setUploading(true);
      setUploadMessage(null);

      await customerImportApi.uploadFile(file);
      setFile(null);

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }

      // Trigger refresh of staging list
      setStagingRefreshTrigger(prev => prev + 1);
    } catch (err: unknown) {
      // Extract error message from API response
      let errorMessage = t('batchImport.uploadFailed');

      if (err && typeof err === 'object' && 'response' in err) {
        const errorData = (err as { response?: { data?: { message?: string; errors?: Record<string, string> } } }).response?.data;

        if (errorData?.message) {
          errorMessage = errorData.message;

          // If there are specific field errors, append them
          if (errorData.errors) {
            const fieldErrors = Object.entries(errorData.errors)
              .map(([field, message]) => `${field}: ${message}`)
              .join(', ');
            errorMessage += ` (${fieldErrors})`;
          }
        }
      } else if (err && typeof err === 'object' && 'message' in err) {
        errorMessage = (err as Error).message;
      }

      setUploadMessage({type: 'error', text: errorMessage});
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleStatsUpdate = (_stats: { valid: number; update: number; duplicate: number; invalid: number }) => {
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
      setCancelling(true);
      await customerImportApi.cancelImport();
      setStagingRefreshTrigger(prev => prev + 1);
      setImportResult(null);
      setUploadMessage(null); // Clear upload message (success or error)
      setFile(null); // Clear selected file
      setStagingStats({valid: 0, update: 0, duplicate: 0, invalid: 0}); // Clear statistics
      setSelectedStatus(null); // Reset filter

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
    } catch (err) {
      console.error('Failed to cancel import:', err);
    } finally {
      setCancelling(false);
    }
  };

  // Show permission denied for non-admin/officer
  if (!canImport) {
    return (
      <div className="min-h-screen bg-gray-50 p-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <AlertCircle className="w-16 h-16 text-orange-500 mx-auto mb-4"/>
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
      <div className="flex-1 overflow-y-auto p-7">
        <div className="mx-auto space-y-4">
          {/* Top Component */}
          <div className="bg-white rounded-lg shadow p-4">
            {/* Header and Action Buttons */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-4">
              <h1 className="text-xl font-semibold text-gray-900">{t('batchImport.uploadExcel')}</h1>

              <div className="flex flex-wrap items-center gap-3">
                <input
                  id="file-upload"
                  type="file"
                  accept=".xlsx"
                  onChange={handleFileSelect}
                  className="hidden"
                />

                <button
                  onClick={() => document.getElementById('file-upload')?.click()}
                  className="btn-secondary focus:outline-none focus:ring-0 active:outline-none active:ring-0 text-sm"
                >
                  {t('batchImport.selectFile')}
                </button>

                {/* Action Buttons */}
                <button
                  onClick={handleCancelImport}
                  className="btn-secondary focus:outline-none focus:ring-0 active:outline-none active:ring-0 text-sm"
                >
                  {t('batchImport.cancelClear')}
                </button>
                <button
                  onClick={handleConfirmImport}
                  disabled={confirming}
                  className="btn-primary focus:outline-none focus:ring-0 active:outline-none active:ring-0 flex items-center gap-2 text-sm"
                >
                  <CheckCircle size={18}/>
                  <span>{confirming ? t('batchImport.importing') : t('batchImport.confirmImport')}</span>
                </button>
              </div>
            </div>

            {/* Second Row - File Info and Actions */}
            {file && (
              <div
                className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4 bg-gray-50 rounded-lg mb-4">
                <div className="flex items-center gap-3 min-w-0">
                  <FileSpreadsheet className="w-5 h-5 text-green-600 flex-shrink-0"/>
                  <span className="text-sm font-medium text-gray-700 truncate">{file.name}</span>
                  <span className="text-xs text-gray-500 flex-shrink-0">({(file.size / 1024).toFixed(2)} KB)</span>
                </div>

                <div className="flex items-center gap-3 flex-shrink-0">
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
                    <Upload size={16}/>
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
              /* Staging Statistics (after file upload) - Clickable to filter */
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div
                  onClick={() => handleStatusFilter('VALID')}
                  className={`bg-green-50 border rounded-lg p-4 cursor-pointer transition-all ${
                    selectedStatus === 'VALID'
                      ? 'border-green-500 ring-2 ring-green-300'
                      : 'border-green-200 hover:border-green-400 hover:shadow-md'
                  }`}
                >
                  <div className="text-2xl font-bold text-green-600">{stagingStats.valid}</div>
                  <div className="text-sm text-green-700">{t('batchImport.newToImport')}</div>
                </div>
                <div
                  onClick={() => handleStatusFilter('UPDATE')}
                  className={`bg-blue-50 border rounded-lg p-4 cursor-pointer transition-all ${
                    selectedStatus === 'UPDATE'
                      ? 'border-blue-500 ring-2 ring-blue-300'
                      : 'border-blue-200 hover:border-blue-400 hover:shadow-md'
                  }`}
                >
                  <div className="text-2xl font-bold text-blue-600">{stagingStats.update}</div>
                  <div className="text-sm text-blue-700">{t('batchImport.toUpdate')}</div>
                </div>
                <div
                  onClick={() => handleStatusFilter('DUPLICATE')}
                  className={`bg-orange-50 border rounded-lg p-4 cursor-pointer transition-all ${
                    selectedStatus === 'DUPLICATE'
                      ? 'border-orange-500 ring-2 ring-orange-300'
                      : 'border-orange-200 hover:border-orange-400 hover:shadow-md'
                  }`}
                >
                  <div className="text-2xl font-bold text-orange-600">{stagingStats.duplicate}</div>
                  <div className="text-sm text-orange-700">{t('batchImport.duplicates')}</div>
                </div>
                <div
                  onClick={() => handleStatusFilter('INVALID')}
                  className={`bg-red-50 border rounded-lg p-4 cursor-pointer transition-all ${
                    selectedStatus === 'INVALID'
                      ? 'border-red-500 ring-2 ring-red-300'
                      : 'border-red-200 hover:border-red-400 hover:shadow-md'
                  }`}
                >
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
            importStatusFilter={selectedStatus}
          />
        </div>
      </div>

      {/* Confirmation Dialog */}
      {showConfirmDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {t('batchImport.confirmNewFileTitle')}
            </h3>
            <p className="text-gray-600 mb-6">
              {t('batchImport.confirmNewFileMessage')}
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={handleConfirmDialogNo}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                {t('batchImport.confirmNewFileNo')}
              </button>
              <button
                onClick={handleConfirmDialogYes}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700"
              >
                {t('batchImport.confirmNewFileYes')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {(uploading || confirming || cancelling) && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-8 max-w-sm mx-4 flex flex-col items-center">
            <Loader2 className="w-12 h-12 text-indigo-600 animate-spin mb-4" />
            <p className="text-lg font-semibold text-gray-900">
              {uploading && t('batchImport.uploading')}
              {confirming && t('batchImport.importing')}
              {cancelling && t('batchImport.cancelling')}
            </p>
            <p className="text-sm text-gray-500 mt-2">
              {t('batchImport.pleaseWait')}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
