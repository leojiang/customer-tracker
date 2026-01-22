package com.example.customers.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Custom validator for phone number format validation.
 *
 * <p>Validates phone numbers by checking if they contain only digits, spaces, dashes, dots,
 * parentheses, or plus sign. Must contain at least 3 digits to be valid.
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

  // Pattern to match phone numbers with digits, spaces, dashes, dots, parentheses, plus
  // Must contain at least 3 digits
  private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\d\\s\\-.()+]+$");

  // Pattern to count digits (at least 3 required)
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");

  @Override
  public void initialize(PhoneNumber constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(String phone, ConstraintValidatorContext context) {
    if (phone == null || phone.trim().isEmpty()) {
      return false;
    }

    // Remove any whitespace
    String cleanPhone = phone.trim();

    // Check if it contains only valid characters (digits, spaces, dashes, dots, parentheses, plus)
    if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
      return false;
    }

    // Count digits - must have at least 3
    long digitCount = DIGIT_PATTERN.matcher(cleanPhone).results().count();

    return digitCount >= 3;
  }
}
