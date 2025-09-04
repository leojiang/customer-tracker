package com.example.customers.repository;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class CustomerSpecifications {

    /**
     * Search by name (case-insensitive contains)
     */
    public static Specification<Customer> hasNameContaining(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + name.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Search by phone (partial match)
     */
    public static Specification<Customer> hasPhoneContaining(String phone) {
        return (root, query, criteriaBuilder) -> {
            if (phone == null || phone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get("phone"),
                "%" + phone.trim() + "%"
            );
        };
    }

    /**
     * Filter by current status
     */
    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("currentStatus"), status);
        };
    }

    /**
     * Include or exclude soft-deleted customers
     * By default, @Where annotation excludes them, but this allows including them
     */
    public static Specification<Customer> includeDeleted(boolean includeDeleted) {
        return (root, query, criteriaBuilder) -> {
            if (includeDeleted) {
                // Override the @Where clause to include deleted records
                return criteriaBuilder.conjunction();
            } else {
                // Explicitly exclude deleted records (redundant with @Where but clearer)
                return criteriaBuilder.isNull(root.get("deletedAt"));
            }
        };
    }

    /**
     * Only soft-deleted customers
     */
    public static Specification<Customer> isDeleted() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNotNull(root.get("deletedAt"));
    }

    /**
     * Only active (non-deleted) customers
     */
    public static Specification<Customer> isActive() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNull(root.get("deletedAt"));
    }

    /**
     * Search by company name (case-insensitive contains)
     */
    public static Specification<Customer> hasCompanyContaining(String company) {
        return (root, query, criteriaBuilder) -> {
            if (company == null || company.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("company")),
                "%" + company.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by sales person phone
     */
    public static Specification<Customer> hasSalesPhone(String salesPhone) {
        return (root, query, criteriaBuilder) -> {
            if (salesPhone == null || salesPhone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("salesPhone"), salesPhone.trim());
        };
    }

    /**
     * Combined search specification
     * Handles all common search parameters at once
     */
    public static Specification<Customer> searchCustomers(
            String nameQuery, 
            String phoneQuery, 
            CustomerStatus status,
            String company,
            String salesPhone,
            boolean includeDeleted) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Name search
            if (nameQuery != null && !nameQuery.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + nameQuery.toLowerCase().trim() + "%"
                ));
            }
            
            // Phone search
            if (phoneQuery != null && !phoneQuery.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    root.get("phone"),
                    "%" + phoneQuery.trim() + "%"
                ));
            }
            
            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("currentStatus"), status));
            }
            
            // Company search
            if (company != null && !company.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("company")),
                    "%" + company.toLowerCase().trim() + "%"
                ));
            }
            
            // Sales phone filter
            if (salesPhone != null && !salesPhone.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("salesPhone"), salesPhone.trim()));
            }
            
            // Deleted status
            if (!includeDeleted) {
                predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Unified search specification
     * Searches across multiple fields with a single query term
     */
    public static Specification<Customer> unifiedSearch(
            String searchQuery, 
            CustomerStatus status,
            String salesPhone,
            boolean includeDeleted) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Unified search across multiple fields
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String lowerQuery = "%" + searchQuery.toLowerCase().trim() + "%";
                
                List<Predicate> searchPredicates = new ArrayList<>();
                
                // Search in name
                searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), lowerQuery
                ));
                
                // Search in phone
                searchPredicates.add(criteriaBuilder.like(
                    root.get("phone"), "%" + searchQuery.trim() + "%"
                ));
                
                // Search in company
                searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("company")), lowerQuery
                ));
                
                // Search in business requirements
                searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("businessRequirements")), lowerQuery
                ));
                
                // Combine search predicates with OR
                predicates.add(criteriaBuilder.or(searchPredicates.toArray(new Predicate[0])));
            }
            
            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("currentStatus"), status));
            }
            
            // Sales phone filter
            if (salesPhone != null && !salesPhone.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("salesPhone"), salesPhone.trim()));
            }
            
            // Deleted status
            if (!includeDeleted) {
                predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}