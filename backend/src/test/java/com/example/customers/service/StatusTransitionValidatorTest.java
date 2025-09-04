package com.example.customers.service;

import com.example.customers.model.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Status Transition Validator Tests")
class StatusTransitionValidatorTest {

    private StatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StatusTransitionValidator();
    }

    @Test
    @DisplayName("Should allow valid transitions from CUSTOMER_CALLED")
    void shouldAllowValidTransitionsFromCustomerCalled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.CUSTOMER_CALLED;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should reject invalid transitions from CUSTOMER_CALLED")
    void shouldRejectInvalidTransitionsFromCustomerCalled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.CUSTOMER_CALLED;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
    }

    @Test
    @DisplayName("Should allow valid transitions from REPLIED_TO_CUSTOMER")
    void shouldAllowValidTransitionsFromRepliedToCustomer() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.REPLIED_TO_CUSTOMER;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should reject invalid transitions from REPLIED_TO_CUSTOMER")
    void shouldRejectInvalidTransitionsFromRepliedToCustomer() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.REPLIED_TO_CUSTOMER;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
    }

    @Test
    @DisplayName("Should allow valid transitions from ORDER_PLACED")
    void shouldAllowValidTransitionsFromOrderPlaced() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_PLACED;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
    }

    @Test
    @DisplayName("Should reject invalid transitions from ORDER_PLACED")
    void shouldRejectInvalidTransitionsFromOrderPlaced() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_PLACED;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should allow valid transitions from ORDER_CANCELLED")
    void shouldAllowValidTransitionsFromOrderCancelled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_CANCELLED;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should reject invalid transitions from ORDER_CANCELLED")
    void shouldRejectInvalidTransitionsFromOrderCancelled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_CANCELLED;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
    }

    @Test
    @DisplayName("Should allow valid transitions from PRODUCT_DELIVERED")
    void shouldAllowValidTransitionsFromProductDelivered() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.PRODUCT_DELIVERED;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
    }

    @Test
    @DisplayName("Should reject invalid transitions from PRODUCT_DELIVERED")
    void shouldRejectInvalidTransitionsFromProductDelivered() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.PRODUCT_DELIVERED;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should reject all transitions from BUSINESS_DONE (terminal state)")
    void shouldRejectAllTransitionsFromBusinessDone() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.BUSINESS_DONE;

        // When & Then - Test all possible transitions are rejected
        for (CustomerStatus toStatus : CustomerStatus.values()) {
            assertFalse(validator.isValidTransition(fromStatus, toStatus),
                String.format("Transition from %s to %s should be invalid", fromStatus, toStatus));
        }
    }

    @Test
    @DisplayName("Should allow valid transitions from LOST")
    void shouldAllowValidTransitionsFromLost() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.LOST;

        // When & Then
        assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.CUSTOMER_CALLED));
    }

    @Test
    @DisplayName("Should reject invalid transitions from LOST")
    void shouldRejectInvalidTransitionsFromLost() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.LOST;

        // When & Then
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.REPLIED_TO_CUSTOMER));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_PLACED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ORDER_CANCELLED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.PRODUCT_DELIVERED));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.BUSINESS_DONE));
        assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.LOST));
    }

    @ParameterizedTest
    @EnumSource(CustomerStatus.class)
    @DisplayName("Should reject same status transitions for all statuses")
    void shouldRejectSameStatusTransitions(CustomerStatus status) {
        // When & Then
        assertFalse(validator.isValidTransition(status, status),
            String.format("Same status transition from %s to %s should be invalid", status, status));
    }

    @Test
    @DisplayName("Should handle null from status")
    void shouldHandleNullFromStatus() {
        // When & Then
        assertFalse(validator.isValidTransition(null, CustomerStatus.CUSTOMER_CALLED));
    }

    @Test
    @DisplayName("Should handle null to status")
    void shouldHandleNullToStatus() {
        // When & Then
        assertFalse(validator.isValidTransition(CustomerStatus.CUSTOMER_CALLED, null));
    }

    @Test
    @DisplayName("Should handle both null statuses")
    void shouldHandleBothNullStatuses() {
        // When & Then
        assertFalse(validator.isValidTransition(null, null));
    }

    @Test
    @DisplayName("Should return correct valid transitions for CUSTOMER_CALLED")
    void shouldReturnCorrectValidTransitionsForCustomerCalled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.CUSTOMER_CALLED;

        // When
        Set<CustomerStatus> validTransitions = validator.getValidTransitions(fromStatus);

        // Then
        assertEquals(2, validTransitions.size());
        assertTrue(validTransitions.contains(CustomerStatus.REPLIED_TO_CUSTOMER));
        assertTrue(validTransitions.contains(CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should return correct valid transitions for ORDER_CANCELLED")
    void shouldReturnCorrectValidTransitionsForOrderCancelled() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_CANCELLED;

        // When
        Set<CustomerStatus> validTransitions = validator.getValidTransitions(fromStatus);

        // Then
        assertEquals(2, validTransitions.size());
        assertTrue(validTransitions.contains(CustomerStatus.ORDER_PLACED));
        assertTrue(validTransitions.contains(CustomerStatus.LOST));
    }

    @Test
    @DisplayName("Should return empty set for BUSINESS_DONE terminal state")
    void shouldReturnEmptySetForBusinessDoneTerminalState() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.BUSINESS_DONE;

        // When
        Set<CustomerStatus> validTransitions = validator.getValidTransitions(fromStatus);

        // Then
        assertTrue(validTransitions.isEmpty());
    }

    @Test
    @DisplayName("Should return empty set for null from status")
    void shouldReturnEmptySetForNullFromStatus() {
        // When
        Set<CustomerStatus> validTransitions = validator.getValidTransitions(null);

        // Then
        assertTrue(validTransitions.isEmpty());
    }

    @Test
    @DisplayName("Should return meaningful error message for invalid transition")
    void shouldReturnMeaningfulErrorMessageForInvalidTransition() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.BUSINESS_DONE;
        CustomerStatus toStatus = CustomerStatus.CUSTOMER_CALLED;

        // When
        String errorMessage = validator.getTransitionErrorMessage(fromStatus, toStatus);

        // Then
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Business done"));
        assertTrue(errorMessage.contains("terminal state"));
    }

    @Test
    @DisplayName("Should return meaningful error message for same status transition")
    void shouldReturnMeaningfulErrorMessageForSameStatusTransition() {
        // Given
        CustomerStatus status = CustomerStatus.ORDER_PLACED;

        // When
        String errorMessage = validator.getTransitionErrorMessage(status, status);

        // Then
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("already in status"));
        assertTrue(errorMessage.contains("Order placed"));
    }

    @Test
    @DisplayName("Should return meaningful error message for backward transition")
    void shouldReturnMeaningfulErrorMessageForBackwardTransition() {
        // Given
        CustomerStatus fromStatus = CustomerStatus.ORDER_PLACED;
        CustomerStatus toStatus = CustomerStatus.CUSTOMER_CALLED;

        // When
        String errorMessage = validator.getTransitionErrorMessage(fromStatus, toStatus);

        // Then
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("Cannot transition from"));
        assertTrue(errorMessage.contains("Order placed"));
        assertTrue(errorMessage.contains("Customer called"));
        assertTrue(errorMessage.contains("Valid transitions are"));
    }

    @Test
    @DisplayName("Should return error message for null statuses")
    void shouldReturnErrorMessageForNullStatuses() {
        // When & Then
        String errorMessage1 = validator.getTransitionErrorMessage(null, CustomerStatus.CUSTOMER_CALLED);
        assertNotNull(errorMessage1);
        assertTrue(errorMessage1.contains("must be specified"));

        String errorMessage2 = validator.getTransitionErrorMessage(CustomerStatus.CUSTOMER_CALLED, null);
        assertNotNull(errorMessage2);
        assertTrue(errorMessage2.contains("must be specified"));

        String errorMessage3 = validator.getTransitionErrorMessage(null, null);
        assertNotNull(errorMessage3);
        assertTrue(errorMessage3.contains("must be specified"));
    }
}