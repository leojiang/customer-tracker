package com.example.customers.service;

import com.example.customers.controller.AnalyticsController.DashboardOverviewResponse;
import com.example.customers.controller.AnalyticsController.LeaderboardResponse;
import com.example.customers.controller.AnalyticsController.PeriodChange;
import com.example.customers.controller.AnalyticsController.RealtimeMetricsResponse;
import com.example.customers.controller.AnalyticsController.SalesPerformanceEntry;
import com.example.customers.controller.AnalyticsController.SalesPerformanceResponse;
import com.example.customers.controller.AnalyticsController.StatusDistributionResponse;
import com.example.customers.controller.AnalyticsController.TrendAnalysisResponse;
import com.example.customers.controller.AnalyticsController.TrendDataPoint;
import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.StatusHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for analytics and dashboard operations.
 *
 * <p>Provides business logic for dashboard metrics, performance tracking, and data aggregation
 * with role-based data filtering.
 */
@Service
public class AnalyticsService {

  private final CustomerRepository customerRepository;
  private final StatusHistoryRepository statusHistoryRepository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public AnalyticsService(
      CustomerRepository customerRepository,
      StatusHistoryRepository statusHistoryRepository,
      JdbcTemplate jdbcTemplate) {
    this.customerRepository = customerRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Get dashboard overview metrics.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @return Dashboard overview data
   */
  public DashboardOverviewResponse getDashboardOverview(String salesPhone, int days) {
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);
    LocalDateTime previousStartDate = startDate.minusDays(days);

    // Current period metrics
    long totalCustomers = getTotalCustomers(salesPhone);
    long newCustomersThisPeriod = getNewCustomersInPeriod(salesPhone, startDate, endDate);
    long activeCustomers = getActiveCustomers(salesPhone);
    BigDecimal conversionRate = getConversionRate(salesPhone, days);

    // Previous period metrics for comparison
    long newCustomersPreviousPeriod = 
        getNewCustomersInPeriod(salesPhone, previousStartDate, startDate);
    BigDecimal previousConversionRate = getConversionRate(salesPhone, days, startDate);

    // Calculate period changes
    PeriodChange periodChange = calculatePeriodChange(
        totalCustomers, 
        newCustomersThisPeriod, 
        newCustomersPreviousPeriod,
        conversionRate, 
        previousConversionRate);

    return new DashboardOverviewResponse(
        totalCustomers,
        newCustomersThisPeriod,
        activeCustomers,
        conversionRate,
        periodChange);
  }

  /**
   * Get customer status distribution.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @return Status distribution data
   */
  public StatusDistributionResponse getStatusDistribution(String salesPhone) {
    String baseQuery = """
        SELECT current_status, COUNT(*) as count 
        FROM customers 
        WHERE deleted_at IS NULL
        """;
    
    List<Object> params = new ArrayList<>();
    if (salesPhone != null) {
      baseQuery += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    baseQuery += " GROUP BY current_status";

    List<Map<String, Object>> results = jdbcTemplate.queryForList(baseQuery, params.toArray());
    
    Map<String, Long> statusCounts = new HashMap<>();
    long totalCustomers = 0;
    
    for (Map<String, Object> row : results) {
      String status = (String) row.get("current_status");
      Long count = ((Number) row.get("count")).longValue();
      statusCounts.put(status, count);
      totalCustomers += count;
    }

    return new StatusDistributionResponse(statusCounts, totalCustomers);
  }

  /**
   * Get customer acquisition trends.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @param granularity Data granularity (daily, weekly)
   * @return Trend analysis data
   */
  public TrendAnalysisResponse getCustomerTrends(String salesPhone, int days, String granularity) {
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);

    String dateFormat = "daily".equals(granularity) ? "DATE(created_at)" : "DATE_TRUNC('week', created_at)::date";
    
    String baseQuery = String.format("""
        SELECT %s as date, COUNT(*) as new_customers
        FROM customers 
        WHERE created_at >= ? AND created_at <= ? AND deleted_at IS NULL
        """, dateFormat);
    
    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    
    if (salesPhone != null) {
      baseQuery += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    baseQuery += String.format(" GROUP BY %s ORDER BY date", dateFormat);

    List<Map<String, Object>> results = jdbcTemplate.queryForList(baseQuery, params.toArray());
    
    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal = getTotalCustomersBeforeDate(salesPhone, startDate);
    
    for (Map<String, Object> row : results) {
      LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
      Long newCustomers = ((Number) row.get("new_customers")).longValue();
      runningTotal += newCustomers;
      
      BigDecimal conversionRateAtDate = getConversionRateAtDate(salesPhone, date);
      
      dataPoints.add(new TrendDataPoint(date, newCustomers, runningTotal, conversionRateAtDate));
    }

    return new TrendAnalysisResponse(dataPoints, granularity, days);
  }

  /**
   * Get sales performance metrics.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @return Sales performance data
   */
  public SalesPerformanceResponse getSalesPerformance(String salesPhone, int days) {
    LocalDateTime startDate = LocalDateTime.now().minusDays(days);

    long totalCustomers = getTotalCustomers(salesPhone);
    long newCustomers = getNewCustomersInPeriod(salesPhone, startDate, LocalDateTime.now());
    long conversions = getConversions(salesPhone, days);
    BigDecimal conversionRate = getConversionRate(salesPhone, days);

    // Get status breakdown
    Map<String, Long> statusBreakdown = getStatusBreakdown(salesPhone);

    return new SalesPerformanceResponse(
        salesPhone != null ? salesPhone : "ADMIN",
        totalCustomers,
        newCustomers,
        conversions,
        conversionRate,
        statusBreakdown);
  }

  /**
   * Get sales team leaderboard (Admin only).
   *
   * @param days Number of days for analysis
   * @param metric Ranking metric (customers, conversions, rate)
   * @return Leaderboard data
   */
  public LeaderboardResponse getSalesLeaderboard(int days, String metric) {
    LocalDateTime startDate = LocalDateTime.now().minusDays(days);

    String query = """
        SELECT s.phone,
               COUNT(DISTINCT c.id) as total_customers,
               COUNT(DISTINCT CASE WHEN c.current_status = 'BUSINESS_DONE' THEN c.id END) as conversions,
               CASE 
                 WHEN COUNT(DISTINCT c.id) > 0 
                 THEN ROUND(COUNT(DISTINCT CASE WHEN c.current_status = 'BUSINESS_DONE' THEN c.id END) * 100.0 / COUNT(DISTINCT c.id), 2)
                 ELSE 0 
               END as conversion_rate
        FROM sales s
        LEFT JOIN customers c ON s.phone = c.sales_phone 
            AND c.created_at >= ? AND c.deleted_at IS NULL
        WHERE s.role = 'SALES'
        GROUP BY s.phone
        """;

    String orderBy = switch (metric.toLowerCase()) {
      case "customers" -> "ORDER BY total_customers DESC";
      case "conversions" -> "ORDER BY conversions DESC";
      case "rate" -> "ORDER BY conversion_rate DESC, total_customers DESC";
      default -> "ORDER BY conversions DESC";
    };

    query += orderBy;

    List<Map<String, Object>> results = jdbcTemplate.queryForList(query, startDate);
    
    List<SalesPerformanceEntry> rankings = new ArrayList<>();
    int rank = 1;
    
    for (Map<String, Object> row : results) {
      String phone = (String) row.get("phone");
      Long totalCustomers = ((Number) row.get("total_customers")).longValue();
      Long conversions = ((Number) row.get("conversions")).longValue();
      BigDecimal conversionRate = (BigDecimal) row.get("conversion_rate");
      
      rankings.add(new SalesPerformanceEntry(
          phone, totalCustomers, conversions, conversionRate, rank++));
    }

    return new LeaderboardResponse(rankings, days, metric);
  }

  /**
   * Get real-time metrics.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @return Real-time metrics data
   */
  public RealtimeMetricsResponse getRealtimeMetrics(String salesPhone) {
    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
    LocalDateTime now = LocalDateTime.now();

    long activeCustomersToday = getActiveCustomersToday(salesPhone);
    long newCustomersToday = getNewCustomersInPeriod(salesPhone, startOfDay, now);
    long conversionsToday = getConversionsInPeriod(salesPhone, startOfDay, now);

    String lastUpdated = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

    return new RealtimeMetricsResponse(
        activeCustomersToday, newCustomersToday, conversionsToday, lastUpdated);
  }

  // Scheduled job to generate daily analytics snapshots
  @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
  public void generateDailySnapshots() {
    // Implementation for future optimization
    // This would populate the analytics_snapshots table
  }

  // Helper methods

  private long getTotalCustomers(String salesPhone) {
    String query = "SELECT COUNT(*) FROM customers WHERE deleted_at IS NULL";
    List<Object> params = new ArrayList<>();
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private long getNewCustomersInPeriod(String salesPhone, LocalDateTime startDate, LocalDateTime endDate) {
    String query = """
        SELECT COUNT(*) FROM customers 
        WHERE created_at >= ? AND created_at <= ? AND deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private long getActiveCustomers(String salesPhone) {
    // Define active customers as those with recent status changes (last 30 days)
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    
    String query = """
        SELECT COUNT(DISTINCT c.id) FROM customers c
        JOIN status_history sh ON c.id = sh.customer_id
        WHERE sh.changed_at >= ? AND c.deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(thirtyDaysAgo);
    
    if (salesPhone != null) {
      query += " AND c.sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private BigDecimal getConversionRate(String salesPhone, int days) {
    return getConversionRate(salesPhone, days, LocalDateTime.now());
  }

  private BigDecimal getConversionRate(String salesPhone, int days, LocalDateTime endDate) {
    LocalDateTime startDate = endDate.minusDays(days);
    
    String query = """
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN current_status = 'BUSINESS_DONE' THEN 1 END) as conversions
        FROM customers 
        WHERE created_at >= ? AND created_at <= ? AND deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    Map<String, Object> result = jdbcTemplate.queryForMap(query, params.toArray());
    
    Long total = ((Number) result.get("total")).longValue();
    Long conversions = ((Number) result.get("conversions")).longValue();
    
    if (total == 0) {
      return BigDecimal.ZERO;
    }
    
    return BigDecimal.valueOf(conversions)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private long getConversions(String salesPhone, int days) {
    LocalDateTime startDate = LocalDateTime.now().minusDays(days);
    
    String query = """
        SELECT COUNT(*) FROM customers 
        WHERE current_status = 'BUSINESS_DONE' AND created_at >= ? AND deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(startDate);
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private long getConversionsInPeriod(String salesPhone, LocalDateTime startDate, LocalDateTime endDate) {
    String query = """
        SELECT COUNT(*) FROM customers 
        WHERE current_status = 'BUSINESS_DONE' 
        AND created_at >= ? AND created_at <= ? AND deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(startDate);
    params.add(endDate);
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private Map<String, Long> getStatusBreakdown(String salesPhone) {
    String query = "SELECT current_status, COUNT(*) as count FROM customers WHERE deleted_at IS NULL";
    List<Object> params = new ArrayList<>();
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    query += " GROUP BY current_status";
    
    List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params.toArray());
    Map<String, Long> breakdown = new HashMap<>();
    
    for (Map<String, Object> row : results) {
      String status = (String) row.get("current_status");
      Long count = ((Number) row.get("count")).longValue();
      breakdown.put(status, count);
    }
    
    return breakdown;
  }

  private long getTotalCustomersBeforeDate(String salesPhone, LocalDateTime date) {
    String query = "SELECT COUNT(*) FROM customers WHERE created_at < ? AND deleted_at IS NULL";
    List<Object> params = new ArrayList<>();
    params.add(date);
    
    if (salesPhone != null) {
      query += " AND sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private BigDecimal getConversionRateAtDate(String salesPhone, LocalDate date) {
    // Get conversion rate up to this date
    return getConversionRate(salesPhone, Integer.MAX_VALUE, date.plusDays(1).atStartOfDay());
  }

  private long getActiveCustomersToday(String salesPhone) {
    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
    
    String query = """
        SELECT COUNT(DISTINCT c.id) FROM customers c
        JOIN status_history sh ON c.id = sh.customer_id
        WHERE sh.changed_at >= ? AND c.deleted_at IS NULL
        """;
    List<Object> params = new ArrayList<>();
    params.add(startOfDay);
    
    if (salesPhone != null) {
      query += " AND c.sales_phone = ?";
      params.add(salesPhone);
    }
    
    return jdbcTemplate.queryForObject(query, params.toArray(), Long.class);
  }

  private PeriodChange calculatePeriodChange(
      long totalCustomers,
      long newCustomersThisPeriod,
      long newCustomersPreviousPeriod, 
      BigDecimal conversionRate,
      BigDecimal previousConversionRate) {

    BigDecimal totalCustomersChange = calculatePercentageChange(
        totalCustomers - newCustomersThisPeriod, totalCustomers);
    BigDecimal newCustomersChange = calculatePercentageChange(
        newCustomersPreviousPeriod, newCustomersThisPeriod);
    BigDecimal conversionRateChange = previousConversionRate.compareTo(BigDecimal.ZERO) == 0 
        ? BigDecimal.ZERO 
        : conversionRate.subtract(previousConversionRate);

    return new PeriodChange(totalCustomersChange, newCustomersChange, conversionRateChange);
  }

  private BigDecimal calculatePercentageChange(long oldValue, long newValue) {
    if (oldValue == 0) {
      return newValue > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
    }
    return BigDecimal.valueOf((newValue - oldValue) * 100.0 / oldValue)
        .setScale(2, RoundingMode.HALF_UP);
  }
}