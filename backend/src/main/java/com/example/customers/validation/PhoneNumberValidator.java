package com.example.customers.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Custom validator for phone number format validation.
 *
 * <p>Validates phone numbers by checking if they contain only digits.
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

  // Pattern to match phone numbers with digits only
  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d+$");

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

    // Check if it contains only digits
    return PHONE_PATTERN.matcher(cleanPhone).matches();
  }
}
