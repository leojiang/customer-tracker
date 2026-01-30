package com.example.customers.model;

/**
 * Customer type classification.
 *
 * <p>Distinguishes between new customer registrations and renewing customers undergoing review
 * process.
 */
public enum CustomerType {
  /** New customer registration (新报名客户) */
  NEW_CUSTOMER,

  /** Renewing customer under review (复审客户) */
  RENEW_CUSTOMER
}
