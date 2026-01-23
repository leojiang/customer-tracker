/**
 * Certificate type utility functions for display and mapping
 */

import { CertificateType, CertificateTypeTranslationKeys } from '@/types/customer';

/**
 * Gets the localized display name for a certificate type
 * @param certificateType - The certificate type enum value
 * @param t - The translation function from useLanguage hook
 * @returns Localized display name for the certificate type
 */
export function getCertificateTypeDisplayName(certificateType: CertificateType, t: any): string {
  const translationKey = CertificateTypeTranslationKeys[certificateType];
  return translationKey ? t(translationKey) : certificateType;
}

/**
 * Gets all available certificate type options for dropdowns
 * @returns Array of certificate type options with enum values as keys
 */
export function getCertificateTypeOptions(): Array<{
  value: CertificateType;
  label: CertificateType;
}> {
  return Object.values(CertificateType).map(value => ({
    value: value as CertificateType,
    label: value as CertificateType
  }));
}