package com.example.customers.service;

import com.example.customers.model.CustomerStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates customer status transitions according to business rules.
 * 
 * Business rules from docs/status_transition.md:
 * - CUSTOMER_CALLED -> REPLIED_TO_CUSTOMER or LOST
 * - REPLIED_TO_CUSTOMER -> ORDER_PLACED or LOST  
 * - ORDER_PLACED -> PRODUCT_DELIVERED or ORDER_CANCELLED
 * - ORDER_CANCELLED -> ORDER_PLACED or LOST
 * - PRODUCT_DELIVERED -> BUSINESS_DONE
 * - BUSINESS_DONE -> (terminal state, no transitions allowed)
 * - LOST -> CUSTOMER_CALLED (restart flow)
 */
@Component
public class StatusTransitionValidator {

    private static final Map<CustomerStatus, Set<CustomerStatus>> VALID_TRANSITIONS = Map.of(
        CustomerStatus.CUSTOMER_CALLED, EnumSet.of(
            CustomerStatus.REPLIED_TO_CUSTOMER,
            CustomerStatus.LOST
        ),
        CustomerStatus.REPLIED_TO_CUSTOMER, EnumSet.of(
            CustomerStatus.ORDER_PLACED,
            CustomerStatus.LOST
        ),
        CustomerStatus.ORDER_PLACED, EnumSet.of(
            CustomerStatus.PRODUCT_DELIVERED,
            CustomerStatus.ORDER_CANCELLED
        ),
        CustomerStatus.ORDER_CANCELLED, EnumSet.of(
            CustomerStatus.ORDER_PLACED,
            CustomerStatus.LOST
        ),
        CustomerStatus.PRODUCT_DELIVERED, EnumSet.of(
            CustomerStatus.BUSINESS_DONE
        ),
        CustomerStatus.BUSINESS_DONE, EnumSet.noneOf(CustomerStatus.class), // Terminal state
        CustomerStatus.LOST, EnumSet.of(
            CustomerStatus.CUSTOMER_CALLED // Restart flow
        )
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
        return validTransitions != null ? EnumSet.copyOf(validTransitions) : EnumSet.noneOf(CustomerStatus.class);
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
        
        Set<CustomerStatus> validTransitions = getValidTransitions(fromStatus);
        if (validTransitions.isEmpty()) {
            return String.format("No status transitions are allowed from %s (terminal state)", 
                fromStatus.getDisplayName());
        }
        
        StringBuilder validOptions = new StringBuilder();
        validTransitions.forEach(status -> {
            if (validOptions.length() > 0) {
                validOptions.append(", ");
            }
            validOptions.append(status.getDisplayName());
        });
        
        return String.format("Cannot transition from %s to %s. Valid transitions are: %s", 
            fromStatus.getDisplayName(), toStatus.getDisplayName(), validOptions.toString());
    }
}