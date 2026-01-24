package com.example.customers.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for identity card numbers.
 *
 * <p>Validates that the id card contains only digits (0-9) and English letters (a-z, A-Z).
 *
 * <p>Empty or null values are considered valid (use @NotBlank or @NotNull for required validation).
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

  private static final String ID_CARD_PATTERN = "^[a-zA-Z0-9]*$";

  @Override
  public void initialize(IdCard constraintAnnotation) {
    // No initialization needed
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null values are considered valid (use @NotNull for null validation)
    if (value == null || value.isEmpty()) {
      return true;
    }

    // Check if the value contains only digits and English letters
    return value.matches(ID_CARD_PATTERN);
  }
}
