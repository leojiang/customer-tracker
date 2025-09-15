package com.example.customers.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Custom validator for phone number format validation.
 *
 * <p>Validates phone numbers according to international standards: - Must start with + followed by
 * country code (1-3 digits) - Followed by 7-15 digits total - Examples: +1234567890, +8612345678901
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

  // International phone number pattern
  // +[country code][number] where country code is 1-3 digits and total digits are 7-15
  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

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

    // Check if it matches the international phone pattern
    return PHONE_PATTERN.matcher(cleanPhone).matches();
  }
}
