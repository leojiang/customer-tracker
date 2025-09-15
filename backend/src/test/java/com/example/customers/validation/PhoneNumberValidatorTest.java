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
  @DisplayName("Should validate correct international phone numbers")
  void shouldValidateCorrectInternationalPhoneNumbers() {
    // Valid phone numbers
    String[] validPhones = {
      "+1234567890", // US format
      "+8612345678901", // China format
      "+44123456789", // UK format
      "+33123456789", // France format
      "+49123456789", // Germany format
      "+123456789012345", // Long format (15 digits)
      "+1234567", // Short format (7 digits)
      "+12345678901234" // Medium format (14 digits)
    };

    for (String phone : validPhones) {
      assertTrue(validator.isValid(phone, context), "Phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject invalid phone number formats")
  void shouldRejectInvalidPhoneNumberFormats() {
    // Invalid phone numbers
    String[] invalidPhones = {
      "1234567890", // Missing + prefix
      "+0123456789", // Country code starting with 0
      "+1234567890123456", // Too long (16 digits)
      "+123456", // Too short (6 digits)
      "+12345678901234567890", // Way too long
      "+123456789012345678901234567890", // Extremely long
      "+", // Just plus sign
      "+1", // Too short
      "+12", // Too short
      "+123", // Too short
      "+1234", // Too short
      "+12345", // Too short
      "+123456", // Too short
      "++1234567890", // Double plus
      "+1234567890a", // Contains letter
      "+1234567890-", // Contains dash
      "+1234567890.", // Contains dot
      "+1234567890(", // Contains parenthesis
      "+1234567890)", // Contains parenthesis
      "+123 456 7890", // Contains spaces
      "+123-456-7890", // Contains dashes
      "+123.456.7890", // Contains dots
      "+123(456)7890", // Contains parentheses
      "+1234567890x123", // Contains extension
      "+1234567890#", // Contains hash
      "+1234567890*", // Contains asterisk
      "+1234567890+", // Contains plus at end
      "+1234567890=", // Contains equals
      "+1234567890!", // Contains exclamation
      "+1234567890@", // Contains at symbol
      "+1234567890$", // Contains dollar sign
      "+1234567890%", // Contains percent
      "+1234567890^", // Contains caret
      "+1234567890&", // Contains ampersand
      "+1234567890_", // Contains underscore
      "+1234567890[", // Contains opening bracket
      "+1234567890]", // Contains closing bracket
      "+1234567890{", // Contains opening brace
      "+1234567890}", // Contains closing brace
      "+1234567890|", // Contains pipe
      "+1234567890\\", // Contains backslash
      "+1234567890:", // Contains colon
      "+1234567890;", // Contains semicolon
      "+1234567890\"", // Contains quote
      "+1234567890'", // Contains apostrophe
      "+1234567890<", // Contains less than
      "+1234567890>", // Contains greater than
      "+1234567890,", // Contains comma
      "+1234567890?", // Contains question mark
      "+1234567890/", // Contains slash
      "+1234567890~", // Contains tilde
      "+1234567890`", // Contains backtick
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
  }

  @Test
  @DisplayName("Should handle phone numbers with leading/trailing whitespace")
  void shouldHandlePhoneNumbersWithLeadingTrailingWhitespace() {
    // Valid phone numbers with whitespace should be trimmed and validated
    assertTrue(
        validator.isValid(" +1234567890 ", context),
        "Phone number with leading/trailing whitespace should be valid");
    assertTrue(
        validator.isValid("\t+1234567890\t", context),
        "Phone number with tab whitespace should be valid");
    assertTrue(
        validator.isValid("\n+1234567890\n", context),
        "Phone number with newline whitespace should be valid");
  }

  @Test
  @DisplayName("Should validate minimum length phone numbers")
  void shouldValidateMinimumLengthPhoneNumbers() {
    // Test minimum valid length (7 digits total)
    assertTrue(
        validator.isValid("+1234567", context), "Minimum length phone number should be valid");
    assertTrue(
        validator.isValid("+12345678", context), "Minimum length + 1 phone number should be valid");
  }

  @Test
  @DisplayName("Should validate maximum length phone numbers")
  void shouldValidateMaximumLengthPhoneNumbers() {
    // Test maximum valid length (15 digits total)
    assertTrue(
        validator.isValid("+123456789012345", context),
        "Maximum length phone number should be valid");
    assertTrue(
        validator.isValid("+12345678901234", context),
        "Maximum length - 1 phone number should be valid");
  }

  @Test
  @DisplayName("Should reject phone numbers that are too short")
  void shouldRejectPhoneNumbersThatAreTooShort() {
    // Test too short (less than 7 digits total)
    assertFalse(
        validator.isValid("+123456", context), "Phone number with 6 digits should be invalid");
    assertFalse(
        validator.isValid("+12345", context), "Phone number with 5 digits should be invalid");
    assertFalse(
        validator.isValid("+1234", context), "Phone number with 4 digits should be invalid");
    assertFalse(validator.isValid("+123", context), "Phone number with 3 digits should be invalid");
    assertFalse(validator.isValid("+12", context), "Phone number with 2 digits should be invalid");
    assertFalse(validator.isValid("+1", context), "Phone number with 1 digit should be invalid");
  }

  @Test
  @DisplayName("Should reject phone numbers that are too long")
  void shouldRejectPhoneNumbersThatAreTooLong() {
    // Test too long (more than 15 digits total)
    assertFalse(
        validator.isValid("+1234567890123456", context),
        "Phone number with 16 digits should be invalid");
    assertFalse(
        validator.isValid("+12345678901234567", context),
        "Phone number with 17 digits should be invalid");
    assertFalse(
        validator.isValid("+123456789012345678", context),
        "Phone number with 18 digits should be invalid");
  }

  @Test
  @DisplayName("Should reject phone numbers with invalid country codes")
  void shouldRejectPhoneNumbersWithInvalidCountryCodes() {
    // Test invalid country codes (starting with 0)
    assertFalse(
        validator.isValid("+0123456789", context),
        "Phone number with country code starting with 0 should be invalid");
    assertFalse(
        validator.isValid("+00123456789", context),
        "Phone number with country code 00 should be invalid");
    assertFalse(
        validator.isValid("+000123456789", context),
        "Phone number with country code 000 should be invalid");
  }

  @Test
  @DisplayName("Should validate various country code lengths")
  void shouldValidateVariousCountryCodeLengths() {
    // Test 1-digit country code
    assertTrue(
        validator.isValid("+1234567890", context),
        "Phone number with 1-digit country code should be valid");

    // Test 2-digit country code
    assertTrue(
        validator.isValid("+44123456789", context),
        "Phone number with 2-digit country code should be valid");

    // Test 3-digit country code
    assertTrue(
        validator.isValid("+1234567890123", context),
        "Phone number with 3-digit country code should be valid");
  }

  @Test
  @DisplayName("Should handle edge cases correctly")
  void shouldHandleEdgeCasesCorrectly() {
    // Test edge cases
    assertTrue(
        validator.isValid("+1234567", context), "Minimum valid phone number should be valid");
    assertTrue(
        validator.isValid("+123456789012345", context),
        "Maximum valid phone number should be valid");

    // Test boundary conditions
    assertFalse(
        validator.isValid("+123456", context), "One digit less than minimum should be invalid");
    assertFalse(
        validator.isValid("+1234567890123456", context),
        "One digit more than maximum should be invalid");
  }

  @Test
  @DisplayName("Should validate real-world phone number examples")
  void shouldValidateRealWorldPhoneNumberExamples() {
    // Real-world examples
    String[] realWorldPhones = {
      "+1234567890", // US
      "+8613800138000", // China Mobile
      "+447911123456", // UK Mobile
      "+33123456789", // France
      "+49123456789", // Germany
      "+81312345678", // Japan
      "+61234567890", // Australia
      "+5511987654321", // Brazil
      "+919876543210", // India
      "+8612345678901" // China
    };

    for (String phone : realWorldPhones) {
      assertTrue(
          validator.isValid(phone, context), "Real-world phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject common invalid formats")
  void shouldRejectCommonInvalidFormats() {
    // Common invalid formats
    String[] commonInvalidFormats = {
      "1234567890", // Missing +
      "(123) 456-7890", // US format with parentheses
      "123-456-7890", // US format with dashes
      "123.456.7890", // US format with dots
      "123 456 7890", // US format with spaces
      "+1 (123) 456-7890", // US format with parentheses and dashes
      "+1-123-456-7890", // US format with dashes
      "+1.123.456.7890", // US format with dots
      "+1 123 456 7890", // US format with spaces
      "1234567890x123", // With extension
      "1234567890#123", // With extension
      "1234567890*123", // With extension
      "1234567890,123", // With extension
      "1234567890;123" // With extension
    };

    for (String phone : commonInvalidFormats) {
      assertFalse(
          validator.isValid(phone, context), "Common invalid format should be rejected: " + phone);
    }
  }
}
