package com.example.customers.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for phone number format.
 *
 * <p>Validates that phone numbers follow international format: +[country code][number] where
 * country code is 1-3 digits and total digits are 7-15.
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

  String message() default "Phone number must be in international format (+1234567890)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
