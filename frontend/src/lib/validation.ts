/**
 * Validation utilities for form inputs
 */

// Phone number pattern - digits only
const PHONE_PATTERN = /^\d+$/;

export interface ValidationResult {
  isValid: boolean;
  message?: string;
}

/**
 * Validates phone number format
 * @param phone - Phone number to validate
 * @returns ValidationResult with isValid and optional message
 */
export function validatePhoneNumber(phone: string): ValidationResult {
  if (!phone || phone.trim() === '') {
    return {
      isValid: false,
      message: 'Phone number is required'
    };
  }

  const cleanPhone = phone.trim();

  if (!PHONE_PATTERN.test(cleanPhone)) {
    return {
      isValid: false,
      message: 'Phone number must contain only digits'
    };
  }

  return {
    isValid: true
  };
}

/**
 * Formats phone number for display
 * @param phone - Raw phone number
 * @returns Formatted phone number
 */
export function formatPhoneNumber(phone: string): string {
  if (!phone) {return '';}

  return phone.trim();
}

/**
 * Validates name field
 * @param name - Name to validate
 * @returns ValidationResult
 */
export function validateName(name: string): ValidationResult {
  if (!name || name.trim() === '') {
    return {
      isValid: false,
      message: 'Name is required'
    };
  }

  if (name.trim().length < 2) {
    return {
      isValid: false,
      message: 'Name must be at least 2 characters long'
    };
  }

  return {
    isValid: true
  };
}

/**
 * Validates age field
 * @param age - Age to validate
 * @returns ValidationResult
 */
export function validateAge(age: number | undefined): ValidationResult {
  if (age === undefined || age === null) {
    return { isValid: true }; // Age is optional
  }

  if (age < 1 || age > 120) {
    return {
      isValid: false,
      message: 'Age must be between 1 and 120'
    };
  }

  return {
    isValid: true
  };
}