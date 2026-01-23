/**
 * Error handling utilities for mapping backend error codes to user-friendly messages
 */

export interface BusinessErrorResponse {
  errorCode: string;
  message: string;
  timestamp: string;
}

export interface ErrorResponse {
  message: string;
  timestamp: string;
}

/**
 * Maps backend error codes to frontend translation keys
 */
export const ERROR_CODE_MAP: Record<string, string> = {
  'DUPLICATE_CUSTOMER_CERTIFICATE': 'error.duplicateCustomerCertificate',
  'PERMISSION_DENIED': 'error.permissionDenied',
};

/**
 * Extracts error information from various response types and returns a user-friendly message
 */
export function getErrorMessage(error: unknown, t: (key: string) => string): string {
  if (typeof error === 'string') {
    return error;
  }

  if (error instanceof Error) {
    // Try to parse as JSON first
    try {
      const parsed = JSON.parse(error.message);
      if (parsed.errorCode && parsed.message) {
        const businessError = parsed as BusinessErrorResponse;
        const translationKey = ERROR_CODE_MAP[businessError.errorCode];
        return translationKey ? t(translationKey) : businessError.message;
      }
      // Handle ErrorResponse (which has message field but no errorCode)
      if (parsed.message && !parsed.errorCode) {
        const errorResponse = parsed as ErrorResponse;
        const translationKey = ERROR_CODE_MAP[errorResponse.message];
        return translationKey ? t(translationKey) : errorResponse.message;
      }
      if (parsed.message) {
        return parsed.message;
      }
    } catch {
      // Not JSON, use the error message directly
      return error.message;
    }
  }

  // Fallback for unknown error types
  return String(error);
}

/**
 * Checks if an error is a specific business error type
 */
export function isBusinessErrorCode(error: unknown, errorCode: string): boolean {
  if (typeof error === 'string') {
    try {
      const parsed = JSON.parse(error);
      return parsed.errorCode === errorCode;
    } catch {
      return false;
    }
  }

  if (error instanceof Error) {
    try {
      const parsed = JSON.parse(error.message);
      return parsed.errorCode === errorCode;
    } catch {
      return false;
    }
  }

  return false;
}
