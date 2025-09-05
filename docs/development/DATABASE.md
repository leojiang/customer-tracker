# Database Design & Management

> **Complete guide to the PostgreSQL database schema, migrations, and optimization**

## 🗄️ **Database Overview**

### **Technology Stack**
- **Database**: PostgreSQL 15+
- **Migration Tool**: Flyway 10.16.0
- **ORM**: JPA/Hibernate with Spring Data
- **Connection Pooling**: HikariCP
- **Deployment**: Docker/Podman containerization

### **📊 Schema Statistics**
- **Tables**: 4 main tables + analytics support
- **Indexes**: 15+ performance-optimized indexes
- **Constraints**: Foreign keys, unique constraints, check constraints
- **Migration Files**: 6 versioned migration scripts
- **Estimated Size**: ~500MB for 100k customers with full analytics

## 🏗️ **Schema Design**

### **📋 Core Tables**

#### **🏢 sales** - Sales User Management
```sql
CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed
    role VARCHAR(10) NOT NULL DEFAULT 'SALES',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT check_sales_role CHECK (role IN ('ADMIN', 'SALES')),
    CONSTRAINT check_sales_phone_format CHECK (phone ~ '^\+?[1-9]\d{10,14}$')
);
```

**Purpose**: Authentication and authorization
**Key Features**:
- Unique phone numbers as primary identifiers
- BCrypt password hashing for security
- Role-based access control (ADMIN/SALES)
- Phone format validation

#### **👥 customers** - Customer Information
```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    company TEXT,
    business_requirements TEXT,
    business_type TEXT,
    age INTEGER CHECK (age > 0 AND age < 150),
    education TEXT,
    gender TEXT,
    location TEXT,
    current_status VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_CALLED',
    sales_phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,  -- Soft delete
    
    CONSTRAINT unique_phone UNIQUE (phone),
    CONSTRAINT fk_customers_sales FOREIGN KEY (sales_phone) REFERENCES sales(phone),
    CONSTRAINT check_customer_status CHECK (current_status IN (
        'CUSTOMER_CALLED', 'REPLIED_TO_CUSTOMER', 'ORDER_PLACED',
        'ORDER_CANCELLED', 'PRODUCT_DELIVERED', 'BUSINESS_DONE', 'LOST'
    ))
);
```

**Purpose**: Core customer data and relationship tracking
**Key Features**:
- Global phone uniqueness (including soft-deleted)
- Soft delete pattern with `deleted_at` timestamp
- Sales person assignment via foreign key
- Status workflow enforcement
- Comprehensive customer demographics

#### **📋 status_history** - Audit Trail
```sql
CREATE TABLE status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    from_status VARCHAR(50),  -- NULL for initial creation
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    sales_phone VARCHAR(20),  -- For analytics performance
    
    CONSTRAINT fk_status_history_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_sales FOREIGN KEY (sales_phone)
        REFERENCES sales(phone)
);
```

**Purpose**: Complete audit trail of all status changes
**Key Features**:
- Immutable audit log
- Reason tracking for business intelligence
- Sales person attribution for analytics
- Cascade delete with customer

#### **📊 analytics_snapshots** - Performance Optimization
```sql
CREATE TABLE analytics_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date DATE NOT NULL,
    sales_phone VARCHAR(20),  -- NULL for system-wide
    metric_type VARCHAR(50) NOT NULL,  -- 'daily', 'weekly', 'monthly'
    
    -- Customer metrics
    total_customers INTEGER DEFAULT 0,
    new_customers INTEGER DEFAULT 0,
    active_customers INTEGER DEFAULT 0,
    converted_customers INTEGER DEFAULT 0,
    lost_customers INTEGER DEFAULT 0,
    
    -- Status distribution  
    customers_called INTEGER DEFAULT 0,
    customers_replied INTEGER DEFAULT 0,
    orders_placed INTEGER DEFAULT 0,
    orders_cancelled INTEGER DEFAULT 0,
    products_delivered INTEGER DEFAULT 0,
    business_done INTEGER DEFAULT 0,
    
    -- Performance metrics
    conversion_rate DECIMAL(5,2),
    avg_cycle_time_days DECIMAL(8,2),
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(snapshot_date, sales_phone, metric_type)
);
```

**Purpose**: Pre-calculated analytics for dashboard performance
**Key Features**:
- Time-series metrics storage
- System-wide and per-sales-person snapshots
- Daily/weekly/monthly aggregations
- Performance optimization for dashboards

## 🚀 **Performance Optimization**

### **📈 Indexing Strategy**

#### **Customer Table Indexes**
```sql
-- Primary search index (GIN for full-text search)
CREATE INDEX idx_customers_search ON customers USING gin(
    to_tsvector('english', name || ' ' || COALESCE(company, '') || ' ' || 
                COALESCE(business_requirements, ''))
) WHERE deleted_at IS NULL;

-- Phone number lookup (exact match)
CREATE INDEX idx_customers_phone ON customers(phone);

-- Status-based queries
CREATE INDEX idx_customers_status ON customers(current_status) WHERE deleted_at IS NULL;

-- Sales person filtering
CREATE INDEX idx_customers_status_sales ON customers(current_status, sales_phone) 
WHERE deleted_at IS NULL;

-- Time-based queries
CREATE INDEX idx_customers_created_at ON customers(created_at DESC) WHERE deleted_at IS NULL;

-- Recently updated customers
CREATE INDEX idx_customers_updated_at ON customers(updated_at DESC) WHERE deleted_at IS NULL;
```

#### **Status History Indexes**
```sql
-- Customer status history lookup
CREATE INDEX idx_status_history_customer_time ON status_history(customer_id, changed_at DESC);

-- Analytics queries by sales person
CREATE INDEX idx_status_history_sales_date ON status_history(sales_phone, changed_at DESC);

-- Time-based analytics
CREATE INDEX idx_status_history_changed_at ON status_history(changed_at DESC);

-- Status transition analysis
CREATE INDEX idx_status_history_to_status ON status_history(to_status, changed_at DESC);
```

#### **Analytics Indexes**
```sql
-- Snapshot retrieval
CREATE INDEX idx_analytics_snapshots_date_sales ON analytics_snapshots(snapshot_date, sales_phone);

-- Metric type filtering
CREATE INDEX idx_analytics_snapshots_type ON analytics_snapshots(metric_type);
```

### **🔧 Query Optimization**

#### **Common Query Patterns**
```sql
-- Optimized customer search (uses GIN index)
SELECT c.* FROM customers c 
WHERE to_tsvector('english', c.name || ' ' || COALESCE(c.company, '')) @@ plainto_tsquery('search term')
  AND c.deleted_at IS NULL
ORDER BY c.updated_at DESC;

-- Status distribution (uses status index)  
SELECT current_status, COUNT(*) 
FROM customers 
WHERE deleted_at IS NULL 
  AND sales_phone = ?  -- Uses composite index
GROUP BY current_status;

-- Customer trends (uses created_at index)
SELECT DATE(created_at) as date, COUNT(*) as new_customers
FROM customers 
WHERE created_at >= ? 
  AND created_at <= ?
  AND deleted_at IS NULL
GROUP BY DATE(created_at)
ORDER BY date;
```

### **📊 Analytics Performance**
- **Pre-aggregated Data**: `analytics_snapshots` table for fast dashboard loading
- **Scheduled Jobs**: Daily batch processing for metrics calculation
- **Caching Strategy**: Redis integration for frequently accessed data (planned)
- **Query Optimization**: JPA query methods optimized for analytics workloads

## 🔄 **Migration Management**

### **📁 Migration Files**

#### **V1__init.sql** - Initial Schema
- Creates core tables (customers, status_history)
- Establishes primary keys and basic constraints
- Sets up initial indexes for performance

#### **V2__convert_enum_to_varchar.sql** - JPA Compatibility
- Converts PostgreSQL enums to VARCHAR for JPA compatibility
- Updates all references and constraints
- Maintains data integrity during conversion

#### **V3__add_sales_table.sql** - Authentication Support
- Creates sales table for user management
- Adds foreign key relationships
- Implements role-based access control

#### **V4__add_audit_triggers.sql** - Audit Trail Enhancement
- Adds automatic `updated_at` triggers
- Implements comprehensive audit logging
- Performance optimizations for audit queries

#### **V5__enhance_search_indexes.sql** - Search Optimization
- Adds GIN indexes for full-text search
- Optimizes common query patterns
- Improves pagination performance

#### **V6__add_analytics_support.sql** - Dashboard Analytics
- Creates analytics_snapshots table
- Adds performance indexes for analytics queries
- Enhances status_history with sales_phone column

### **🔧 Migration Best Practices**

#### **Creating New Migrations**
```bash
# Naming convention: V{N}__{description}.sql
touch backend/src/main/resources/db/migration/V7__add_customer_notes.sql
```

#### **Migration Template**
```sql
-- V7__add_customer_notes.sql
-- Purpose: Add notes field for customer additional information

-- Add new column
ALTER TABLE customers ADD COLUMN notes TEXT;

-- Add index for search
CREATE INDEX idx_customers_notes ON customers USING gin(to_tsvector('english', notes));

-- Update existing records (if needed)
-- UPDATE customers SET notes = '' WHERE notes IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN customers.notes IS 'Additional notes and comments about the customer';

-- ROLLBACK INSTRUCTIONS (in comments):
-- DROP INDEX idx_customers_notes;
-- ALTER TABLE customers DROP COLUMN notes;
```

#### **Safe Migration Guidelines**
1. **Backup Before Migration** - Always backup production data
2. **Test on Copy** - Run migration on production copy first
3. **Monitor Performance** - Check for slow queries after migration
4. **Document Rollback** - Include rollback instructions in comments
5. **Incremental Changes** - Prefer smaller, focused migrations

## 🔍 **Database Monitoring**

### **📊 Performance Metrics**

#### **Query Performance**
```sql
-- Enable query statistics
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Monitor slow queries
SELECT query, mean_exec_time, calls, total_exec_time
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

#### **Connection Monitoring**
```sql
-- Active connections
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE state = 'active';

-- Connection details
SELECT usename, application_name, client_addr, state, query_start
FROM pg_stat_activity 
WHERE state != 'idle';
```

#### **Storage Monitoring**
```sql
-- Table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(tablename::text)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(tablename::text) DESC;

-- Database size
SELECT pg_size_pretty(pg_database_size('customers')) as database_size;
```

### **🔧 Maintenance Tasks**

#### **Regular Maintenance**
```sql
-- Analyze tables for query planner
ANALYZE;

-- Vacuum tables to reclaim space
VACUUM ANALYZE customers;
VACUUM ANALYZE status_history;

-- Reindex if needed
REINDEX TABLE customers;
```

#### **Scheduled Maintenance**
```bash
#!/bin/bash
# Daily maintenance script

psql -d customers -c "VACUUM ANALYZE;"
psql -d customers -c "SELECT pg_stat_reset();" # Reset statistics weekly
```

## 🔐 **Security & Access Control**

### **🛡️ Database Security**

#### **User Management**
```sql
-- Application user (limited permissions)
CREATE USER app_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE customers TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Read-only user (for reporting)
CREATE USER readonly_user WITH PASSWORD 'readonly_password';  
GRANT CONNECT ON DATABASE customers TO readonly_user;
GRANT USAGE ON SCHEMA public TO readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
```

#### **Row-Level Security** (Advanced)
```sql
-- Enable RLS for multi-tenant support
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- Policy for sales users (see only their customers)
CREATE POLICY sales_customer_policy ON customers 
FOR ALL TO app_user
USING (sales_phone = current_setting('myapp.current_user_phone'));

-- Policy for admin users (see all customers)  
CREATE POLICY admin_customer_policy ON customers
FOR ALL TO app_user  
USING (current_setting('myapp.current_user_role') = 'ADMIN');
```

### **🔒 Data Protection**

#### **Backup Strategy**
```bash
# Daily automated backups
pg_dump -h localhost -U postgres -d customers \
  --format=custom \
  --compress=9 \
  --file=backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
pg_restore -h localhost -U postgres -d customers_restore backup_file.sql
```

#### **Sensitive Data Handling**
- **Phone Numbers**: Treated as PII with proper access controls
- **Password Storage**: BCrypt hashing with strong salt
- **Audit Logs**: Immutable with tamper detection
- **Data Retention**: Configurable retention policies for compliance

## 📊 **Analytics Schema**

### **🎯 Analytics Tables Design**

#### **Pre-calculated Metrics**
The `analytics_snapshots` table stores pre-calculated metrics for fast dashboard loading:

```sql
-- Example daily snapshot
INSERT INTO analytics_snapshots (
    snapshot_date, sales_phone, metric_type,
    total_customers, new_customers, active_customers,
    converted_customers, conversion_rate
) VALUES (
    '2025-09-05', '18980994001', 'daily',
    45, 3, 12, 8, 17.78
);
```

#### **Snapshot Generation**
```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void generateDailySnapshots() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    
    // Generate system-wide snapshot
    generateSnapshot(yesterday, null, "daily");
    
    // Generate per-sales-person snapshots
    List<Sales> salesUsers = salesRepository.findByRole(SalesRole.SALES);
    for (Sales sales : salesUsers) {
        generateSnapshot(yesterday, sales.getPhone(), "daily");
    }
}
```

### **📈 Query Patterns for Analytics**

#### **Dashboard Overview Query**
```java
@Query("""
    SELECT COUNT(c) as totalCustomers,
           COUNT(CASE WHEN c.createdAt >= :startDate THEN 1 END) as newCustomers,
           COUNT(CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN 1 END) as conversions
    FROM Customer c 
    WHERE c.deletedAt IS NULL 
      AND (:salesPhone IS NULL OR c.salesPhone = :salesPhone)
""")
Object[] getDashboardMetrics(@Param("salesPhone") String salesPhone, 
                           @Param("startDate") LocalDateTime startDate);
```

#### **Status Distribution Query**  
```java
@Query("""
    SELECT c.currentStatus, COUNT(c)
    FROM Customer c 
    WHERE c.deletedAt IS NULL 
      AND (:salesPhone IS NULL OR c.salesPhone = :salesPhone)
    GROUP BY c.currentStatus
""")
List<Object[]> getStatusDistribution(@Param("salesPhone") String salesPhone);
```

#### **Trend Analysis Query**
```java
@Query("""
    SELECT DATE(c.createdAt) as date, COUNT(c) as count
    FROM Customer c 
    WHERE c.deletedAt IS NULL 
      AND c.createdAt BETWEEN :startDate AND :endDate
      AND (:salesPhone IS NULL OR c.salesPhone = :salesPhone)
    GROUP BY DATE(c.createdAt)
    ORDER BY DATE(c.createdAt)
""")  
List<Object[]> getCustomerTrends(@Param("salesPhone") String salesPhone,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
```

## 🔄 **Data Lifecycle Management**

### **📅 Data Retention Policies**

#### **Customer Data**
- **Active Customers**: Retained indefinitely
- **Soft-Deleted Customers**: Retained for 7 years (compliance)
- **Status History**: Retained permanently (audit requirement)
- **Analytics Snapshots**: Retained for 5 years

#### **Cleanup Procedures**
```sql
-- Cleanup old soft-deleted customers (after 7 years)
DELETE FROM customers 
WHERE deleted_at IS NOT NULL 
  AND deleted_at < NOW() - INTERVAL '7 years';

-- Archive old analytics snapshots (after 5 years)
DELETE FROM analytics_snapshots
WHERE created_at < NOW() - INTERVAL '5 years';
```

### **📦 Data Archiving**

#### **Archive Strategy**
```sql
-- Create archive tables
CREATE TABLE customers_archive (LIKE customers INCLUDING ALL);
CREATE TABLE status_history_archive (LIKE status_history INCLUDING ALL);

-- Move old data to archive
INSERT INTO customers_archive 
SELECT * FROM customers 
WHERE deleted_at IS NOT NULL 
  AND deleted_at < NOW() - INTERVAL '5 years';
```

## 🛠️ **Development Database Setup**

### **🐳 Docker/Podman Setup**
```bash
# Start PostgreSQL container
podman run --name postgres-dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=customers \
  -p 5432:5432 \
  -v postgres-data:/var/lib/postgresql/data \
  -d postgres:15

# Connect to database
psql -h localhost -U postgres -d customers
```

### **🔧 Local Development**
```properties
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/customers
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### **🧪 Test Database**
```properties
# application-test.properties  
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
```

## 📊 **Production Considerations**

### **🚀 Deployment Configuration**
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-db:5432/customers
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.jpa.show-sql=false

# Connection pool optimization  
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
```

### **📈 Scalability Planning**

#### **Database Scaling Strategy**
1. **Vertical Scaling** - Increase CPU/RAM for single instance
2. **Read Replicas** - Separate read and write operations
3. **Connection Pooling** - Optimize connection usage
4. **Query Optimization** - Regular performance analysis and tuning

#### **Partitioning Strategy** (Future)
```sql
-- Partition status_history by date for performance
CREATE TABLE status_history_y2025m09 PARTITION OF status_history
FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

-- Partition analytics_snapshots by date
CREATE TABLE analytics_snapshots_y2025 PARTITION OF analytics_snapshots  
FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

### **🔍 Monitoring & Alerting**

#### **Key Metrics to Monitor**
- **Connection Count** - Monitor connection pool usage
- **Query Performance** - Track slow queries and execution plans
- **Storage Growth** - Monitor database size and table growth
- **Index Usage** - Ensure indexes are being utilized effectively
- **Lock Contention** - Monitor for blocking queries

#### **Automated Monitoring**
```sql
-- Create monitoring views
CREATE VIEW monitoring_slow_queries AS
SELECT query, mean_exec_time, calls, total_exec_time
FROM pg_stat_statements 
WHERE mean_exec_time > 1000  -- Queries taking > 1 second
ORDER BY mean_exec_time DESC;

CREATE VIEW monitoring_table_sizes AS  
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(tablename::text)) as size,
       pg_total_relation_size(tablename::text) as size_bytes
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY size_bytes DESC;
```

## 🛠️ **Troubleshooting**

### **🔍 Common Issues**

#### **Connection Issues**
```bash
# Check PostgreSQL status
podman ps | grep postgres

# Test connection
psql -h localhost -U postgres -d customers -c "SELECT version();"

# Check logs
podman logs postgres-dev
```

#### **Migration Issues**
```bash
# Check migration status
mvn flyway:info

# Repair failed migration
mvn flyway:repair

# Rollback migration (manual)
mvn flyway:undo  # If undo migrations are available
```

#### **Performance Issues**
```sql
-- Check running queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query 
FROM pg_stat_activity 
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';

-- Check locks
SELECT * FROM pg_locks WHERE NOT granted;

-- Check index usage
SELECT relname, seq_scan, seq_tup_read, idx_scan, idx_tup_fetch 
FROM pg_stat_user_tables;
```

---

## 📚 **Additional Resources**

### **🔗 Documentation Links**
- **PostgreSQL Docs**: https://www.postgresql.org/docs/
- **Flyway Documentation**: https://flywaydb.org/documentation/
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Hibernate Guide**: https://hibernate.org/orm/documentation/

### **🛠️ Tools**
- **pgAdmin**: GUI for database management
- **DBeaver**: Universal database client
- **pg_stat_statements**: Query performance monitoring
- **pg_badger**: Log analysis tool

---

**🎯 This database design provides a solid foundation for current needs while enabling future scalability and advanced analytics capabilities.**