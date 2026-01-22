package com.example.customers.service;

import com.example.customers.model.CustomerStatus;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates customer status transitions according to business rules.
 *
 * <p>Business rules:
 *
 * <ul>
 *   <li>NEW → Can transition to any status (NOTIFIED, ABORTED, SUBMITTED, CERTIFIED)
 *   <li>NOTIFIED → Can transition to any non-NEW status (NOTIFIED, ABORTED, SUBMITTED, CERTIFIED)
 *   <li>ABORTED → Can transition to any non-NEW status (NOTIFIED, ABORTED, SUBMITTED, CERTIFIED)
 *   <li>SUBMITTED → Can transition to any non-NEW status (NOTIFIED, ABORTED, SUBMITTED, CERTIFIED)
 *   <li>CERTIFIED → Can transition to any non-NEW status (NOTIFIED, ABORTED, SUBMITTED, CERTIFIED)
 * </ul>
 *
 * <p>Key rule: Once a customer leaves NEW status, they can never return to NEW.
 */
@Component
public class StatusTransitionValidator {

  // All non-NEW statuses - can transition to each other freely
  private static final Set<CustomerStatus> NON_NEW_STATUSES =
      EnumSet.of(
          CustomerStatus.NOTIFIED,
          CustomerStatus.ABORTED,
          CustomerStatus.SUBMITTED,
          CustomerStatus.CERTIFIED);

  private static final Map<CustomerStatus, Set<CustomerStatus>> VALID_TRANSITIONS =
      Map.of(
          CustomerStatus.NEW, NON_NEW_STATUSES, // NEW can transition to all other statuses
          CustomerStatus.NOTIFIED, NON_NEW_STATUSES, // Can transition to any non-NEW status
          CustomerStatus.ABORTED, NON_NEW_STATUSES, // Can transition to any non-NEW status
          CustomerStatus.SUBMITTED, NON_NEW_STATUSES, // Can transition to any non-NEW status
          CustomerStatus.CERTIFIED, NON_NEW_STATUSES // Can transition to any non-NEW status
          );

  /**
   * Validates if a status transition is allowed according to business rules.
   *
   * @param fromStatus Current status
   * @param toStatus Target status
   * @return true if transition is valid, false otherwise
   */
  public boolean isValidTransition(CustomerStatus fromStatus, CustomerStatus toStatus) {
    if (fromStatus == null || toStatus == null) {
      return false;
    }

    // Same status transition is not allowed
    if (fromStatus.equals(toStatus)) {
      return false;
    }

    Set<CustomerStatus> allowedTransitions = VALID_TRANSITIONS.get(fromStatus);
    return allowedTransitions != null && allowedTransitions.contains(toStatus);
  }

  /**
   * Gets all valid status transitions from a given status.
   *
   * @param fromStatus Current status
   * @return Set of valid target statuses
   */
  public Set<CustomerStatus> getValidTransitions(CustomerStatus fromStatus) {
    if (fromStatus == null) {
      return EnumSet.noneOf(CustomerStatus.class);
    }

    Set<CustomerStatus> validTransitions = VALID_TRANSITIONS.get(fromStatus);
    return validTransitions != null
        ? EnumSet.copyOf(validTransitions)
        : EnumSet.noneOf(CustomerStatus.class);
  }

  /**
   * Gets a human-readable error message for an invalid transition.
   *
   * @param fromStatus Current status
   * @param toStatus Target status
   * @return Error message explaining why the transition is invalid
   */
  public String getTransitionErrorMessage(CustomerStatus fromStatus, CustomerStatus toStatus) {
    if (fromStatus == null || toStatus == null) {
      return "Both current and target status must be specified";
    }

    if (fromStatus.equals(toStatus)) {
      return String.format("Customer is already in status: %s", toStatus.getDisplayName());
    }

    // Special error message for trying to return to NEW status
    if (toStatus == CustomerStatus.NEW && fromStatus != CustomerStatus.NEW) {
      return String.format(
          "Cannot transition from %s to NEW. Once a customer leaves NEW status, they cannot return to it.",
          fromStatus.getDisplayName());
    }

    Set<CustomerStatus> validTransitions = getValidTransitions(fromStatus);
    if (validTransitions.isEmpty()) {
      return String.format(
          "No status transitions are allowed from %s", fromStatus.getDisplayName());
    }

    StringBuilder validOptions = new StringBuilder();
    validTransitions.forEach(
        status -> {
          if (validOptions.length() > 0) {
            validOptions.append(", ");
          }
          validOptions.append(status.getDisplayName());
        });

    return String.format(
        "Cannot transition from %s to %s. Valid transitions are: %s",
        fromStatus.getDisplayName(), toStatus.getDisplayName(), validOptions.toString());
  }
}
