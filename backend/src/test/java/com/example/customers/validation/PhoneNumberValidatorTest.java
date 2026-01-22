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
      "1234567890",
      "1234567",
      "12345678901234567890",
      "123",
      "123456789012345678901234567890",
      "1",
      "12"
    };

    for (String phone : validPhones) {
      assertTrue(validator.isValid(phone, context), "Phone number should be valid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with non-digit characters")
  void shouldRejectPhoneNumbersWithNonDigitCharacters() {
    // Invalid phone numbers - contains non-digit characters
    String[] invalidPhones = {
      "123-456-7890",
      "(123) 456-7890",
      "+1234567890",
      "+1 (123) 456-7890",
      "123 456 7890",
      "123.456.7890",
      "+1-123-456-7890",
      "+1 123 456 7890",
      "(123)4567890",
      "+44 (7911) 123456",
      "1234567890a",
      "1234567890x123",
      "1234567890#",
      "1234567890*",
      "1234567890+",
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
  @DisplayName("Should validate single digit")
  void shouldValidateSingleDigit() {
    assertTrue(validator.isValid("1", context), "Single digit should be valid");
  }

  @Test
  @DisplayName("Should validate two digits")
  void shouldValidateTwoDigits() {
    assertTrue(validator.isValid("12", context), "Two digits should be valid");
  }

  @Test
  @DisplayName("Should validate three digits")
  void shouldValidateThreeDigits() {
    assertTrue(validator.isValid("123", context), "Three digits should be valid");
  }

  @Test
  @DisplayName("Should validate very long phone numbers")
  void shouldValidateVeryLongPhoneNumbers() {
    assertTrue(
        validator.isValid("123456789012345678901234567890", context),
        "Very long phone number with digits should be valid");
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

  @Test
  @DisplayName("Should reject phone numbers with spaces")
  void shouldRejectPhoneNumbersWithSpaces() {
    String[] phonesWithSpaces = {"123 456", "123 456 7890", "1 2 3", " 123 456 "};

    for (String phone : phonesWithSpaces) {
      assertFalse(
          validator.isValid(phone, context),
          "Phone number with spaces should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with dashes")
  void shouldRejectPhoneNumbersWithDashes() {
    String[] phonesWithDashes = {"123-456", "123-456-7890", "1-2-3", "-123", "123-", "123-456-"};

    for (String phone : phonesWithDashes) {
      assertFalse(
          validator.isValid(phone, context),
          "Phone number with dashes should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with dots")
  void shouldRejectPhoneNumbersWithDots() {
    String[] phonesWithDots = {"123.456", "123.456.7890", "1.2.3", ".123", "123.", "123.456."};

    for (String phone : phonesWithDots) {
      assertFalse(
          validator.isValid(phone, context), "Phone number with dots should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with parentheses")
  void shouldRejectPhoneNumbersWithParentheses() {
    String[] phonesWithParens = {
      "(123)", "(123)456", "123(456)", "(123)456(789)", "()", "(123", "123)"
    };

    for (String phone : phonesWithParens) {
      assertFalse(
          validator.isValid(phone, context),
          "Phone number with parentheses should be invalid: " + phone);
    }
  }

  @Test
  @DisplayName("Should reject phone numbers with plus sign")
  void shouldRejectPhoneNumbersWithPlusSign() {
    String[] phonesWithPlus = {
      "+123", "+1234567890", "+1-123-456-7890", "++123", "123+", "+", "123+456"
    };

    for (String phone : phonesWithPlus) {
      assertFalse(
          validator.isValid(phone, context), "Phone number with plus should be invalid: " + phone);
    }
  }
}
