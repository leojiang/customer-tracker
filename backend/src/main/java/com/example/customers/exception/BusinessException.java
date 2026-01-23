package com.example.customers.exception;

/**
 * Business exception with error codes for frontend mapping.
 *
 * <p>Used for business logic violations that need specific error handling on the frontend.
 */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Business error codes that can be mapped to specific frontend error messages.
   */
  public enum ErrorCode {
    DUPLICATE_CUSTOMER_CERTIFICATE,
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    PERMISSION_DENIED
  }
}