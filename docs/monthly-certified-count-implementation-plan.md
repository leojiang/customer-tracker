# Monthly Certified Count Implementation Plan

## Overview
This document outlines the implementation of a monthly certified count tracking system. The system will maintain a count of customers who transition to CERTIFIED status each month, enabling efficient analytics and reporting.

## Requirements

### Functional Requirements
1. Create a new table `monthly_certified_count` to store monthly certification counts
2. Primary key should be the month in format `yyyy-mm` (e.g., '2024-01')
3. When a customer transitions to CERTIFIED status, increment the count for the corresponding month
4. If no record exists for that month, create a new record with count = 1
5. Backfill existing data from the `customers` table

### Technical Requirements
- Use Flyway migration for database schema changes
- Implement transactional data integrity
- Handle concurrent updates safely
- Provide data migration script for existing customers

---

## Database Schema

### Table: `monthly_certified_count`

```sql
CREATE TABLE monthly_certified_count (
    month VARCHAR(7) PRIMARY KEY COMMENT 'Month in yyyy-mm format (e.g., 2024-01)',
    certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of customers certified in this month',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT valid_month_format CHECK (month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT non_negative_count CHECK (certified_count >= 0)
) COMMENT='Monthly count of customers who reached CERTIFIED status';

-- Index for querying by month range
CREATE INDEX idx_monthly_certified_count_month ON monthly_certified_count(month);
```

**Field Descriptions:**
- `month`: Primary key, format 'yyyy-mm' (e.g., '2024-01', '2024-12')
- `certified_count`: Total number of customers certified in this month
- `created_at`: Timestamp when the record was created
- `updated_at`: Timestamp when the record was last updated

---

## Implementation Plan

### Phase 1: Database Migration

#### Step 1.1: Create Migration File
**File:** `backend/src/main/resources/db/migration/V11__create_monthly_certified_count_table.sql`

```sql
-- Create monthly_certified_count table
CREATE TABLE monthly_certified_count (
    month VARCHAR(7) PRIMARY KEY COMMENT 'Month in yyyy-mm format (e.g., 2024-01)',
    certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of customers certified in this month',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT valid_month_format CHECK (month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT non_negative_count CHECK (certified_count >= 0)
) COMMENT='Monthly count of customers who reached CERTIFIED status';

-- Index for efficient querying by month range
CREATE INDEX idx_monthly_certified_count_month ON monthly_certified_count(month);
```

#### Step 1.2: Note on Data Migration
**IMPORTANT:** Automatic backfill will NOT be performed via Flyway migration. Instead, use the standalone SQL script provided in Phase 4 to manually populate the table with existing data after deployment.

---

### Phase 2: Backend Implementation

#### Step 2.1: Create Entity Class
**File:** `backend/src/main/java/com/example/customers/entity/MonthlyCertifiedCount.java`

```java
package com.example.customers.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_certified_count")
public class MonthlyCertifiedCount {

    @Id
    @Column(name = "month", nullable = false, length = 7)
    private String month;

    @Column(name = "certified_count", nullable = false)
    private Integer certifiedCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Integer getCertifiedCount() {
        return certifiedCount;
    }

    public void setCertifiedCount(Integer certifiedCount) {
        this.certifiedCount = certifiedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

#### Step 2.2: Create Repository
**File:** `backend/src/main/java/com/example/customers/repository/MonthlyCertifiedCountRepository.java`

```java
package com.example.customers.repository;

import com.example.customers.entity.MonthlyCertifiedCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface MonthlyCertifiedCountRepository extends JpaRepository<MonthlyCertifiedCount, String> {

    /**
     * Increment certified count for a specific month
     * If record doesn't exist, creates a new one with count = 1
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO monthly_certified_count (month, certified_count, created_at, updated_at)
        VALUES (:month, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE
            certified_count = certified_count + 1,
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void incrementCertifiedCount(@Param("month") String month);

    /**
     * Find monthly certified count by month
     */
    Optional<MonthlyCertifiedCount> findByMonth(String month);
}
```

**Key Design Decision:** Using `INSERT ... ON DUPLICATE KEY UPDATE` ensures thread-safe atomic operations without needing explicit locking.

#### Step 2.3: Update CustomerService
**File:** `backend/src/main/java/com/example/customers/service/CustomerService.java`

**Location:** In the method that updates customer status to CERTIFIED (likely `updateCustomerStatus` or similar)

```java
@Service
@Transactional
public class CustomerService {

    @Autowired
    private MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

    // ... existing code ...

    private void updateCustomerStatus(Customer customer, CustomerStatus newStatus, String reason, String salesPhone) {
        CustomerStatus oldStatus = customer.getCurrentStatus();

        // Update customer status
        customer.setCurrentStatus(newStatus);

        // If transitioning to CERTIFIED, set certified_at and increment monthly count
        if (newStatus == CustomerStatus.CERTIFIED && oldStatus != CustomerStatus.CERTIFIED) {
            // Set certified_at to current date if not already set
            if (customer.getCertifiedAt() == null || customer.getCertifiedAt().isEmpty()) {
                String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                customer.setCertifiedAt(currentDate);
            }

            // Increment monthly certified count
            String month = extractMonthFromDate(customer.getCertifiedAt());
            monthlyCertifiedCountRepository.incrementCertifiedCount(month);
        }

        // Save status history
        saveStatusHistory(customer, oldStatus, newStatus, reason, salesPhone);

        // ... rest of the logic ...
    }

    /**
     * Extract month (yyyy-mm) from date string (yyyy-mm-dd)
     */
    private String extractMonthFromDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 7) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
        return dateStr.substring(0, 7); // Returns "yyyy-mm" from "yyyy-mm-dd"
    }
}
```

**Important Considerations:**
1. **Idempotency:** The code checks if the customer is already CERTIFIED to avoid double-counting
2. **Date Extraction:** Extracts month from the certified_at date (not current date) for accuracy
3. **Transaction Safety:** The operation is wrapped in the existing @Transactional method

---

### Phase 3: Testing Strategy

#### Step 3.1: Unit Tests
**File:** `backend/src/test/java/com/example/customers/repository/MonthlyCertifiedCountRepositoryTest.java`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MonthlyCertifiedCountRepositoryTest {

    @Autowired
    private MonthlyCertifiedCountRepository repository;

    @Test
    @Transactional
    void testIncrementCertifiedCount_NewMonth() {
        // Test creating a new month record
        repository.incrementCertifiedCount("2024-01");

        Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-01");
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getCertifiedCount());
    }

    @Test
    @Transactional
    void testIncrementCertifiedCount_ExistingMonth() {
        // First increment
        repository.incrementCertifiedCount("2024-01");
        // Second increment
        repository.incrementCertifiedCount("2024-01");

        Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-01");
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getCertifiedCount());
    }

    @Test
    void testValidMonthFormat() {
        // Test that only valid yyyy-mm formats are accepted
        MonthlyCertifiedCount count = new MonthlyCertifiedCount();
        count.setMonth("2024-01");
        count.setCertifiedCount(5);

        // Should save successfully
        assertDoesNotThrow(() -> repository.save(count));
    }
}
```

#### Step 3.2: Integration Tests
**File:** `backend/src/test/java/com/example/customers/service/CustomerServiceTest.java`

```java
@SpringBootTest
@Transactional
class CustomerServiceStatusUpdateIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testStatusTransitionToCertified_UpdatesMonthlyCount() {
        // Create a customer
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setPhone("1234567890");
        customer.setCurrentStatus(CustomerStatus.NEW);
        customer = customerRepository.save(customer);

        // Get initial count for current month
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long initialCount = monthlyCertifiedCountRepository.findByMonth(currentMonth)
            .map(MonthlyCertifiedCount::getCertifiedCount)
            .orElse(0);

        // Update status to CERTIFIED
        customerService.updateCustomerStatus(
            customer.getId(),
            CustomerStatus.CERTIFIED,
            "Test certification",
            "18980994196"
        );

        // Verify count incremented
        Optional<MonthlyCertifiedCount> updatedCount =
            monthlyCertifiedCountRepository.findByMonth(currentMonth);

        assertTrue(updatedCount.isPresent());
        assertEquals(initialCount + 1, updatedCount.get().getCertifiedCount());
    }

    @Test
    void testStatusTransitionToCertified_AlreadyCertified_NoDoubleCount() {
        // Create a customer already certified
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setPhone("1234567890");
        customer.setCurrentStatus(CustomerStatus.CERTIFIED);
        customer.setCertifiedAt("2024-01-15");
        customer = customerRepository.save(customer);

        String month = "2024-01";
        repository.incrementCertifiedCount(month); // Set initial count

        long initialCount = monthlyCertifiedCountRepository.findByMonth(month)
            .map(MonthlyCertifiedCount::getCertifiedCount)
            .orElse(0);

        // Try to update to CERTIFIED again (should not increment)
        customerService.updateCustomerStatus(
            customer.getId(),
            CustomerStatus.CERTIFIED,
            "Test",
            "18980994196"
        );

        // Verify count didn't change
        Optional<MonthlyCertifiedCount> updatedCount =
            monthlyCertifiedCountRepository.findByMonth(month);

        assertEquals(initialCount, updatedCount.get().getCertifiedCount());
    }
}
```

---

### Phase 4: Data Migration Script (Manual Execution)

#### Step 4.1: Create Standalone Migration Script
**File:** `backend/src/main/resources/db/migration/scripts/backfill_monthly_certified_count.sql`

```sql
-- ============================================================================
-- BACKFILL MONTHLY CERTIFIED COUNT
-- ============================================================================
-- Purpose: Aggregate existing certified customers by month
-- Usage: Run this script manually AFTER deploying the database migration
-- IMPORTANT: This script will EMPTY the monthly_certified_count table before backfilling
-- ============================================================================

-- Step 1: Preview existing certified customers (DRY RUN)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    COUNT(*) AS count
FROM customers
WHERE certified_at IS NOT NULL
  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m')
ORDER BY month DESC
LIMIT 20;

-- Step 2: EMPTY the monthly_certified_count table (FRESH START)
-- WARNING: This will DELETE all existing data in the table!
TRUNCATE TABLE monthly_certified_count;

-- Step 3: Insert aggregated data (BACKFILL)
INSERT INTO monthly_certified_count (month, certified_count)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    COUNT(*) AS certified_count
FROM customers
WHERE certified_at IS NOT NULL
  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m')
ORDER BY month;

-- Step 4: Verification - Check inserted data
SELECT
    month,
    certified_count,
    created_at,
    updated_at
FROM monthly_certified_count
ORDER BY month DESC
LIMIT 20;

-- Step 5: Summary statistics
SELECT
    COUNT(*) AS total_months,
    SUM(certified_count) AS total_certified,
    MIN(month) AS earliest_month,
    MAX(month) AS latest_month
FROM monthly_certified_count;

-- Step 6: Data integrity check - verify counts match customer table
SELECT
    mcc.month,
    mcc.certified_count AS table_count,
    COUNT(c.id) AS actual_count,
    (mcc.certified_count - COUNT(c.id)) AS difference
FROM monthly_certified_count mcc
LEFT JOIN customers c ON DATE_FORMAT(c.certified_at, '%Y-%m') = mcc.month
    AND c.current_status = 'CERTIFIED'
    AND c.deleted_at IS NULL
GROUP BY mcc.month, mcc.certified_count
HAVING table_count != actual_count;
-- If this query returns any rows, there's a data mismatch that needs investigation
```

**Execution Instructions:**
1. **CRITICAL:** Backup database before running this script
2. Run in a test environment first to verify results
3. Review the preview (Step 1) to understand what data will be inserted
4. Execute the entire script at once for consistency
5. Verify results using Step 5 and Step 6
6. Keep a record of the execution for audit purposes

**Notes:**
- The `TRUNCATE TABLE` command ensures a clean slate before backfilling
- This approach prevents duplicate or stale data
- The script includes verification queries to ensure data integrity
- Run this script ONLY ONCE after the initial deployment

---

## Deployment Checklist

### Pre-Deployment
- [ ] Review migration file V11 (table creation only)
- [ ] Test migration in development environment
- [ ] Prepare the standalone backfill SQL script
- [ ] Run unit and integration tests (without backfill data)
- [ ] Performance test with large datasets

### Deployment Steps
1. [ ] **CRITICAL:** Backup production database
2. [ ] Deploy application with migration V11 (table creation)
3. [ ] Verify table creation: `SHOW CREATE TABLE monthly_certified_count;`
4. [ ] **IMPORTANT:** Do NOT run the backfill script yet - table should be empty
5. [ ] Verify application starts successfully
6. [ ] Test certification workflow with one customer
7. [ ] Verify monthly count increments correctly
8. [ ] **MANUAL STEP:** Execute the backfill SQL script in production
9. [ ] Verify backfill results: `SELECT * FROM monthly_certified_count ORDER BY month DESC LIMIT 10;`
10. [ ] Monitor application logs for errors
11. [ ] Full end-to-end testing with real certification workflow

### Post-Deployment
- [ ] Verify new certifications increment count correctly
- [ ] Check for any performance impact
- [ ] Run data integrity verification queries
- [ ] Schedule periodic reconciliation checks
- [ ] Update documentation

---

## Rollback Plan

If issues occur after deployment:

### Option 1: Code Rollback
1. Revert code changes to CustomerService
2. Keep the table (no impact on existing functionality)

### Option 2: Full Rollback
```sql
-- Drop the monthly_certified_count table
DROP TABLE IF EXISTS monthly_certified_count;

-- Remove migrations from Flyway history (use with caution)
-- DELETE FROM flyway_schema_history WHERE version IN ('V11', 'V12');
```

---

## Performance Considerations

### Indexes
- Primary key on `month` provides automatic indexing
- Additional index `idx_monthly_certified_count_month` for range queries

### Query Optimization
- `INSERT ... ON DUPLICATE KEY UPDATE` is atomic and efficient
- No explicit locking required
- Single database operation per certification

### Scalability
- Table size: One row per month (minimal growth)
- Expected rows: ~12 per year, ~120 for 10 years
- No performance degradation expected over time

---

## Monitoring and Maintenance

### Health Check Queries
```sql
-- Check for gaps in monthly data
SELECT
    DATE_FORMAT(DATE_ADD(month, INTERVAL 1 MONTH), '%Y-%m') AS expected_next_month,
    (SELECT month FROM monthly_certified_count mcc2
     WHERE mcc2.month > mcc1.month
     ORDER BY mcc2.month ASC LIMIT 1) AS actual_next_month
FROM monthly_certified_count mcc1
WHERE DATE_ADD(month, INTERVAL 1 MONTH) < (
    SELECT MAX(month) FROM monthly_certified_count
)
HAVING actual_next_month IS NULL;

-- Verify counts match customer table
SELECT
    mcc.month,
    mcc.certified_count AS table_count,
    COUNT(c.id) AS actual_count,
    (mcc.certified_count - COUNT(c.id)) AS difference
FROM monthly_certified_count mcc
LEFT JOIN customers c ON DATE_FORMAT(c.certified_at, '%Y-%m') = mcc.month
    AND c.current_status = 'CERTIFIED'
    AND c.deleted_at IS NULL
GROUP BY mcc.month, mcc.certified_count
HAVING table_count != actual_count;
```

### Data Reconciliation
If discrepancies found, run reconciliation script:
```sql
-- Reset counts for specific month
UPDATE monthly_certified_count
SET certified_count = (
    SELECT COUNT(*)
    FROM customers
    WHERE DATE_FORMAT(certified_at, '%Y-%m') = monthly_certified_count.month
        AND current_status = 'CERTIFIED'
        AND deleted_at IS NULL
)
WHERE month = '2024-01'; -- Replace with specific month
```

---

## API Extensions (Future Enhancement)

Consider adding REST endpoints for querying monthly certified counts:

```java
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @GetMapping("/monthly-certified-count")
    public ResponseEntity<List<MonthlyCertifiedCount>> getMonthlyCertifiedCount(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        // Implementation
    }
}
```

---

## Summary

This implementation provides:
1. ✅ Reliable monthly certification tracking
2. ✅ Atomic, thread-safe updates
3. ✅ Manual backfill of historical data (controlled execution)
4. ✅ Minimal performance overhead
5. ✅ Easy to maintain and monitor
6. ✅ Rollback-friendly design

**Key Differences from Original Plan:**
- ❌ No automatic backfill via Flyway migration
- ✅ Manual SQL script for backfill execution
- ✅ Script includes `TRUNCATE TABLE` to ensure clean slate
- ✅ Verification and integrity checks built into the script

**Total Estimated Effort:** 3-5 hours
- Database Migration (V11 only): 0.5 hour
- Backend Implementation: 2-3 hours
- Testing: 1 hour
- Manual Backfill Execution: 0.5 hour
