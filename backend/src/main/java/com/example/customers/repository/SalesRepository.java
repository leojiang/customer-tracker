package com.example.customers.repository;

import com.example.customers.model.Sales;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Sales entity operations.
 *
 * <p>Provides CRUD operations and authentication-related queries for sales users.
 */
@Repository
public interface SalesRepository extends JpaRepository<Sales, UUID> {

  Optional<Sales> findByPhone(String phone);

  boolean existsByPhone(String phone);
}
