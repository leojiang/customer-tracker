/**
 * Certificate issuer utility functions for mapping and validation
 */

import { CertificateIssuer } from '@/types/customer';

/**
 * Maps existing certificate issuer values to the predefined dropdown options
 * Handles backward compatibility by mapping unknown values to '其它' (Other)
 */
export function mapCertificateIssuerToDisplay(value: string | undefined | null): CertificateIssuer {
  if (!value || value.trim() === '') {
    return CertificateIssuer.OTHER;
  }

  const normalizedValue = value.trim();

  // Direct match with enum values
  const enumValues = Object.values(CertificateIssuer);
  for (const enumValue of enumValues) {
    if (enumValue === normalizedValue) {
      return enumValue as CertificateIssuer;
    }
  }

  // Case-insensitive matching for robustness
  const lowerValue = normalizedValue.toLowerCase();
  for (const enumValue of enumValues) {
    if (enumValue.toLowerCase() === lowerValue) {
      return enumValue as CertificateIssuer;
    }
  }

  // Default to 'Other' if no match found
  return CertificateIssuer.OTHER;
}

/**
 * Gets the display name for a certificate issuer value
 * Ensures consistent display across the application
 */
export function getCertificateIssuerDisplayName(value: string | undefined | null): string {
  if (!value || value.trim() === '') {
    return CertificateIssuer.OTHER;
  }

  const mappedValue = mapCertificateIssuerToDisplay(value);
  return mappedValue;
}

/**
 * Validates if a certificate issuer value matches one of the predefined options
 */
export function isValidCertificateIssuer(value: string): boolean {
  const mappedValue = mapCertificateIssuerToDisplay(value);
  return Object.values(CertificateIssuer).includes(mappedValue);
}

/**
 * Gets all available certificate issuer options for dropdowns
 */
export function getCertificateIssuerOptions(): Array<{
  value: CertificateIssuer;
  label: CertificateIssuer;
}> {
  return Object.values(CertificateIssuer).map(value => ({
    value: value as CertificateIssuer,
    label: value as CertificateIssuer
  }));
}