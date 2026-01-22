package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.customers.model.CustomerStatus;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Status Transition Validator Tests")
class StatusTransitionValidatorTest {

  private StatusTransitionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StatusTransitionValidator();
  }

  @Test
  @DisplayName("Should allow all transitions from NEW status")
  void shouldAllowAllTransitionsFromNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NEW;

    // When & Then - NEW can transition to all other statuses
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.NOTIFIED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ABORTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.SUBMITTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should reject transition from NEW to NEW")
  void shouldRejectTransitionFromNewToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NEW;

    // When & Then - Cannot stay in same status
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NEW));
  }

  @Test
  @DisplayName("Should allow transitions from NOTIFIED to non-NEW statuses")
  void shouldAllowTransitionsFromNotifiedToNonNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NOTIFIED;

    // When & Then - Can transition to any non-NEW status
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ABORTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.SUBMITTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should reject transition from NOTIFIED to NEW")
  void shouldRejectTransitionFromNotifiedToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NOTIFIED;

    // When & Then - Cannot return to NEW
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NEW));
  }

  @Test
  @DisplayName("Should reject transition from NOTIFIED to NOTIFIED")
  void shouldRejectTransitionFromNotifiedToNotified() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NOTIFIED;

    // When & Then - Cannot stay in same status
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NOTIFIED));
  }

  @Test
  @DisplayName("Should allow transitions from ABORTED to non-NEW statuses")
  void shouldAllowTransitionsFromAbortedToNonNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.ABORTED;

    // When & Then - Can transition to any non-NEW status
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.NOTIFIED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.SUBMITTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should reject transition from ABORTED to NEW")
  void shouldRejectTransitionFromAbortedToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.ABORTED;

    // When & Then - Cannot return to NEW
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NEW));
  }

  @Test
  @DisplayName("Should reject transition from ABORTED to ABORTED")
  void shouldRejectTransitionFromAbortedToAborted() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.ABORTED;

    // When & Then - Cannot stay in same status
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.ABORTED));
  }

  @Test
  @DisplayName("Should allow transitions from SUBMITTED to non-NEW statuses")
  void shouldAllowTransitionsFromSubmittedToNonNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.SUBMITTED;

    // When & Then - Can transition to any non-NEW status
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.NOTIFIED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ABORTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should reject transition from SUBMITTED to NEW")
  void shouldRejectTransitionFromSubmittedToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.SUBMITTED;

    // When & Then - Cannot return to NEW
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NEW));
  }

  @Test
  @DisplayName("Should reject transition from SUBMITTED to SUBMITTED")
  void shouldRejectTransitionFromSubmittedToSubmitted() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.SUBMITTED;

    // When & Then - Cannot stay in same status
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.SUBMITTED));
  }

  @Test
  @DisplayName("Should allow transitions from CERTIFIED to non-NEW statuses")
  void shouldAllowTransitionsFromCertifiedToNonNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.CERTIFIED;

    // When & Then - Can transition to any non-NEW status
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.NOTIFIED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.ABORTED));
    assertTrue(validator.isValidTransition(fromStatus, CustomerStatus.SUBMITTED));
  }

  @Test
  @DisplayName("Should reject transition from CERTIFIED to NEW")
  void shouldRejectTransitionFromCertifiedToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.CERTIFIED;

    // When & Then - Cannot return to NEW
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.NEW));
  }

  @Test
  @DisplayName("Should reject transition from CERTIFIED to CERTIFIED")
  void shouldRejectTransitionFromCertifiedToCertified() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.CERTIFIED;

    // When & Then - Cannot stay in same status
    assertFalse(validator.isValidTransition(fromStatus, CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should reject null status transitions")
  void shouldRejectNullStatusTransitions() {
    // Given
    CustomerStatus validStatus = CustomerStatus.NEW;

    // When & Then
    assertFalse(validator.isValidTransition(null, validStatus));
    assertFalse(validator.isValidTransition(validStatus, null));
    assertFalse(validator.isValidTransition(null, null));
  }

  @Test
  @DisplayName("Should return all valid transitions from NEW")
  void shouldReturnAllValidTransitionsFromNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NEW;

    // When
    Set<CustomerStatus> validTransitions = validator.getValidTransitions(fromStatus);

    // Then - Should include all non-NEW statuses
    assertEquals(4, validTransitions.size());
    assertTrue(validTransitions.contains(CustomerStatus.NOTIFIED));
    assertTrue(validTransitions.contains(CustomerStatus.ABORTED));
    assertTrue(validTransitions.contains(CustomerStatus.SUBMITTED));
    assertTrue(validTransitions.contains(CustomerStatus.CERTIFIED));
  }

  @Test
  @DisplayName("Should return all valid transitions from non-NEW statuses")
  void shouldReturnAllValidTransitionsFromNonNewStatuses() {
    // Given
    CustomerStatus[] nonNewStatuses = {
      CustomerStatus.NOTIFIED,
      CustomerStatus.ABORTED,
      CustomerStatus.SUBMITTED,
      CustomerStatus.CERTIFIED
    };

    // When & Then - All non-NEW statuses should have same valid transitions
    for (CustomerStatus fromStatus : nonNewStatuses) {
      Set<CustomerStatus> validTransitions = validator.getValidTransitions(fromStatus);
      assertEquals(4, validTransitions.size());
      assertFalse(validTransitions.contains(CustomerStatus.NEW));
    }
  }

  @Test
  @DisplayName("Should return empty set for null status")
  void shouldReturnEmptySetForNullStatus() {
    // When
    Set<CustomerStatus> validTransitions = validator.getValidTransitions(null);

    // Then
    assertTrue(validTransitions.isEmpty());
  }

  @Test
  @DisplayName("Should provide correct error message for invalid transition to NEW")
  void shouldProvideCorrectErrorMessageForInvalidTransitionToNew() {
    // Given
    CustomerStatus fromStatus = CustomerStatus.NOTIFIED;
    CustomerStatus toStatus = CustomerStatus.NEW;

    // When
    String errorMessage = validator.getTransitionErrorMessage(fromStatus, toStatus);

    // Then
    assertTrue(errorMessage.contains("Cannot transition"));
    assertTrue(errorMessage.contains("NEW"));
    assertTrue(errorMessage.contains("cannot return to it"));
  }

  @Test
  @DisplayName("Should provide correct error message for same status transition")
  void shouldProvideCorrectErrorMessageForSameStatusTransition() {
    // Given
    CustomerStatus status = CustomerStatus.NEW;

    // When
    String errorMessage = validator.getTransitionErrorMessage(status, status);

    // Then
    assertTrue(errorMessage.contains("already in status"));
    assertTrue(errorMessage.contains("New"));
  }

  @Test
  @DisplayName("Should provide correct error message for null statuses")
  void shouldProvideCorrectErrorMessageForNullStatuses() {
    // When
    String errorMessage = validator.getTransitionErrorMessage(null, null);

    // Then
    assertTrue(errorMessage.contains("must be specified"));
  }

  @Test
  @DisplayName("Should allow transitions between all non-NEW statuses")
  void shouldAllowTransitionsBetweenAllNonNewStatuses() {
    // Given
    CustomerStatus[] nonNewStatuses = {
      CustomerStatus.NOTIFIED,
      CustomerStatus.ABORTED,
      CustomerStatus.SUBMITTED,
      CustomerStatus.CERTIFIED
    };

    // When & Then - All non-NEW statuses can transition to each other
    for (CustomerStatus fromStatus : nonNewStatuses) {
      for (CustomerStatus toStatus : nonNewStatuses) {
        if (!fromStatus.equals(toStatus)) {
          assertTrue(
              validator.isValidTransition(fromStatus, toStatus),
              String.format("Should allow transition from %s to %s", fromStatus, toStatus));
        }
      }
    }
  }
}
