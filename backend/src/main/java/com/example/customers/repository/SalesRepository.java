package com.example.customers.repository;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  /** Find all sales users with SALES role. */
  List<Sales> findByRole(SalesRole role);

  // ========== Analytics Query Methods ==========

  /** Get sales leaderboard data with customer and conversion metrics. */
  @Query("""
    SELECT s.phone,
           COUNT(DISTINCT c.id) as totalCustomers,
           COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) as conversions,
           CASE 
             WHEN COUNT(DISTINCT c.id) > 0 
             THEN ROUND(COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) * 100.0 / COUNT(DISTINCT c.id), 2)
             ELSE 0 
           END as conversionRate
    FROM Sales s
    LEFT JOIN Customer c ON s.phone = c.salesPhone 
        AND c.createdAt >= :startDate 
        AND c.deletedAt IS NULL
    WHERE s.role = 'SALES'
    GROUP BY s.phone
    ORDER BY 
      CASE 
        WHEN :metric = 'customers' THEN COUNT(DISTINCT c.id)
        WHEN :metric = 'rate' THEN 
          CASE 
            WHEN COUNT(DISTINCT c.id) > 0 
            THEN COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) * 100.0 / COUNT(DISTINCT c.id)
            ELSE 0 
          END
        ELSE COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END)
      END DESC,
      COUNT(DISTINCT c.id) DESC
    """)
  List<Object[]> getSalesLeaderboardData(@Param("startDate") LocalDateTime startDate, @Param("metric") String metric);
}
