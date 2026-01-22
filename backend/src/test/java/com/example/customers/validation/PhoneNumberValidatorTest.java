package com.example.customers.validation;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phone Number Validator Tests")
class PhoneNumberValidatorTest {

  private PhoneNumberValidator validator;

  @Mock private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new PhoneNumberValidator();
  }

  @Test
  @DisplayName("Should validate phone numbers with digits only")
  void shouldValidatePhoneNumbersWithDigitsOnly() {
    // Valid phone numbers - digits only
    String[] validPhones = {
      "1234567890", "1234567", "12345678901234567890", "123", "123456789012345678901234567890"
    };

    for (String phone : validPhones) {
      assertTrue(validator.isValid(phone, context), "Phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should validate phone numbers with spaces, dashes, and parentheses")
  void shouldValidatePhoneNumbersWithSpacesDashesAndParentheses() {
    // Valid phone numbers with formatting
    String[] validPhones = {
      "123 456 7890",
      "123-456-7890",
      "(123) 456-7890",
      "+1234567890",
      "+1 (123) 456-7890",
      "+86 138 0138 0000",
      "123-456-7890",
      "123.456.7890",
      "+1-123-456-7890",
      "+1 123 456 7890",
      "(123)4567890",
      "+44 (7911) 123456"
    };

    for (String phone : validPhones) {
      assertTrue(validator.isValid(phone, context), "Phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with too few digits")
  void shouldRejectPhoneNumbersWithTooFewDigits() {
    // Invalid phone numbers - less than 3 digits
    String[] invalidPhones = {"", "1", "12", " 1", " 12 ", "-", "()", "+"};

    for (String phone : invalidPhones) {
      assertFalse(validator.isValid(phone, context), "Phone number should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with invalid characters")
  void shouldRejectPhoneNumbersWithInvalidCharacters() {
    // Invalid phone numbers - contains invalid characters
    String[] invalidPhones = {
      "1234567890a",
      "1234567890x123",
      "1234567890#",
      "1234567890*",
      "1234567890=",
      "1234567890!",
      "1234567890@",
      "abc1234567",
      "123-456-7890a",
      "123 456 7890b",
      "123/456/7890",
      "123|456|7890"
    };

    for (String phone : invalidPhones) {
      assertFalse(validator.isValid(phone, context), "Phone number should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject null phone number")
  void shouldRejectNullPhoneNumber() {
    assertFalse(validator.isValid(null, context), "Null phone number should be invalid");
  }

  @Test
  @DisplayName("Should reject empty phone number")
  void shouldRejectEmptyPhoneNumber() {
    assertFalse(validator.isValid("", context), "Empty phone number should be invalid");
  }

  @Test
  @DisplayName("Should reject whitespace-only phone number")
  void shouldRejectWhitespaceOnlyPhoneNumber() {
    assertFalse(
        validator.isValid("   ", context), "Whitespace-only phone number should be invalid");
    assertFalse(validator.isValid("\t\t", context), "Tab-only phone number should be invalid");
    assertFalse(validator.isValid("\n\n", context), "Newline-only phone number should be invalid");
  }

  @Test
  @DisplayName("Should handle phone numbers with leading/trailing whitespace")
  void shouldHandlePhoneNumbersWithLeadingTrailingWhitespace() {
    // Valid phone numbers with whitespace should be trimmed and validated
    assertTrue(
        validator.isValid(" 1234567890 ", context),
        "Phone number with leading/trailing whitespace should be valid");
    assertTrue(
        validator.isValid("\t1234567890\t", context),
        "Phone number with tab whitespace should be valid");
    assertTrue(
        validator.isValid("\n1234567890\n", context),
        "Phone number with newline whitespace should be valid");
  }

  @Test
  @DisplayName("Should validate phone numbers with exactly 3 digits")
  void shouldValidatePhoneNumbersWithExactly3Digits() {
    assertTrue(
        validator.isValid("123", context), "Phone number with exactly 3 digits should be valid");
    assertTrue(
        validator.isValid(" 123 ", context),
        "Phone number with 3 digits and spaces should be valid");
    assertTrue(
        validator.isValid("1-2-3", context),
        "Phone number with 3 digits and dashes should be valid");
  }

  @Test
  @DisplayName("Should validate various formats with minimum digits")
  void shouldValidateVariousFormatsWithMinimumDigits() {
    // Test minimum valid digits (3)
    assertTrue(validator.isValid("123", context), "Simple 3 digits should be valid");
    assertTrue(validator.isValid("1 2 3", context), "3 digits with spaces should be valid");
    assertTrue(validator.isValid("1-2-3", context), "3 digits with dashes should be valid");
    assertTrue(
        validator.isValid("(1)2-3", context),
        "3 digits with parentheses and dashes should be valid");
  }

  @Test
  @DisplayName("Should validate real-world phone number examples")
  void shouldValidateRealWorldPhoneNumberExamples() {
    // Real-world examples with various formats
    String[] realWorldPhones = {
      "1234567890", // Simple digits
      "+1234567890", // With plus
      "+1 (123) 456-7890", // US format
      "+86 138 0138 0000", // China format
      "+44 7911 123456", // UK format
      "(123) 456-7890", // US format without plus
      "123-456-7890", // US format with dashes
      "123.456.7890", // US format with dots
      "123 456 7890", // US format with spaces
      "+49 123 456 789", // Germany format
      "+33123456789", // France format
      "+61 2 3456 7890", // Australia format
      "+55 11 98765 4321", // Brazil format
      "+91 98765 43210" // India format
    };

    for (String phone : realWorldPhones) {
      assertTrue(
          validator.isValid(phone, context), "Real-world phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should handle edge cases correctly")
  void shouldHandleEdgeCasesCorrectly() {
    // Test edge cases
    assertTrue(
        validator.isValid("123", context), "Minimum valid phone number (3 digits) should be valid");
    assertTrue(
        validator.isValid("123456789012345678901234567890", context),
        "Long phone number with many digits should be valid");
    assertTrue(validator.isValid("+123", context), "Plus with 3 digits should be valid");
    assertTrue(validator.isValid("(123)", context), "3 digits in parentheses should be valid");

    // Test boundary conditions
    assertFalse(validator.isValid("12", context), "2 digits should be invalid");
    assertFalse(validator.isValid("1", context), "1 digit should be invalid");
  }

  @Test
  @DisplayName("Should validate phone numbers with mixed formatting")
  void shouldValidatePhoneNumbersWithMixedFormatting() {
    // Phone numbers with various formatting characters
    String[] mixedFormatPhones = {
      "123-456-7890",
      "(123) 456-7890",
      "123 456 7890",
      "123.456.7890",
      "+1-123-456-7890",
      "+1 (123) 456-7890",
      "+1 123 456 7890",
      "(123)-456-7890",
      "123 - 456 - 7890"
    };

    for (String phone : mixedFormatPhones) {
      assertTrue(
          validator.isValid(phone, context), "Mixed format phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with letters")
  void shouldRejectPhoneNumbersWithLetters() {
    String[] phonesWithLetters = {
      "123-456-7890a",
      "123-456-7890ext",
      "123-456-7890x123",
      "abc1234567",
      "123abc4567",
      "1234567abc"
    };

    for (String phone : phonesWithLetters) {
      assertFalse(
          validator.isValid(phone, context),
          "Phone number with letters should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with special characters")
  void shouldRejectPhoneNumbersWithSpecialCharacters() {
    String[] phonesWithSpecialChars = {
      "123#456", "123@456", "123&456", "123*456", "123%456", "123$456", "123^456", "123|456",
      "123~456", "123`456"
    };

    for (String phone : phonesWithSpecialChars) {
      assertFalse(
          validator.isValid(phone, context),
          "Phone number with special characters should be invalid: " + phone);
    }
  }
}
