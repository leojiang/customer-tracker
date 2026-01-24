package com.example.customers.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validation annotation for identity card numbers.
 *
 * <p>Validates that the id card contains only digits (0-9) and English letters (a-z, A-Z).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @IdCard(message = "Identity card must contain only digits and English letters")
 * private String idCard;
 * }</pre>
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = IdCardValidator.class)
@Documented
public @interface IdCard {

  String message() default "Identity card must contain only digits and English letters";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
