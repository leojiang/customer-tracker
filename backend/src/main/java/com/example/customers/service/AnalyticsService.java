package com.example.customers.service;

import com.example.customers.controller.AnalyticsController.CertificateTypeTrendsResponse;
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
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.StatusHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for analytics and dashboard operations.
 *
 * <p>Provides business logic for dashboard metrics, performance tracking, and data aggregation with
 * role-based data filtering.
 */
@Service
public class AnalyticsService {

  private final CustomerRepository customerRepository;
  private final SalesRepository salesRepository;
  private final StatusHistoryRepository statusHistoryRepository;

  @Autowired
  public AnalyticsService(
      CustomerRepository customerRepository,
      SalesRepository salesRepository,
      StatusHistoryRepository statusHistoryRepository) {
    this.customerRepository = customerRepository;
    this.salesRepository = salesRepository;
    this.statusHistoryRepository = statusHistoryRepository;
  }

  /**
   * Get dashboard overview metrics.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @return Dashboard overview data
   */
  public DashboardOverviewResponse getDashboardOverview(String salesPhone, int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);
    ZonedDateTime previousStartDate = startDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();
    String previousStartDateStr = previousStartDate.toLocalDate().toString();
    String startDateComparisonStr = startDate.toLocalDate().toString();

    // Current period metrics
    long totalCustomers =
        (salesPhone != null)
            ? customerRepository.countTotalActiveCustomersBySales(salesPhone)
            : customerRepository.countTotalActiveCustomers();

    long newCustomersThisPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, startDateStr, endDateStr)
            : customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long activeCustomers = getActiveCustomers(salesPhone);
    BigDecimal conversionRate = calculateConversionRate(salesPhone, totalCustomers);

    // Previous period metrics for comparison
    long newCustomersPreviousPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, previousStartDateStr, startDateComparisonStr)
            : customerRepository.countNewCustomersInPeriod(previousStartDateStr, startDateComparisonStr);

    long previousTotalCustomers = totalCustomers - newCustomersThisPeriod;
    BigDecimal previousConversionRate = calculateConversionRate(salesPhone, previousTotalCustomers);

    // Calculate period changes
    PeriodChange periodChange =
        calculatePeriodChange(
            totalCustomers,
            newCustomersThisPeriod,
            newCustomersPreviousPeriod,
            conversionRate,
            previousConversionRate);

    return new DashboardOverviewResponse(
        totalCustomers, newCustomersThisPeriod, activeCustomers, conversionRate, periodChange);
  }

  /**
   * Get customer status distribution.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @return Status distribution data
   */
  public StatusDistributionResponse getStatusDistribution(String salesPhone) {
    List<Object[]> results =
        (salesPhone != null)
            ? customerRepository.countCustomersByStatusForSales(salesPhone)
            : customerRepository.countCustomersByStatus();

    Map<String, Long> statusCounts = new HashMap<>();
    long totalCustomers = 0;

    for (Object[] row : results) {
      CustomerStatus status = (CustomerStatus) row[0];
      Long count = ((Number) row[1]).longValue();
      statusCounts.put(status.name(), count);
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
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    // Debug: Check how many customers have certifiedAt dates
    long totalCustomers = customerRepository.countTotalActiveCustomers();
    System.out.println("DEBUG: Total active customers: " + totalCustomers);

    // Choose query based on granularity
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);
    List<Object[]> results;

    if (isMonthly) {
      results = (salesPhone != null)
          ? customerRepository.getCustomerTrendsByMonthForSales(salesPhone, startDateStr, endDateStr)
          : customerRepository.getCustomerTrendsByMonth(startDateStr, endDateStr);
    } else {
      results = (salesPhone != null)
          ? customerRepository.getCustomerTrendsByDateForSales(salesPhone, startDateStr, endDateStr)
          : customerRepository.getCustomerTrendsByDate(startDateStr, endDateStr);
    }

    System.out.println("DEBUG: Trends Query - startDate: " + startDateStr + ", endDate: " + endDateStr + ", granularity: " + granularity);
    System.out.println("DEBUG: Trends Query - results size: " + results.size());
    for (Object[] row : results) {
      System.out.println("DEBUG: Trends Query - period: " + row[0] + ", count: " + row[1]);
    }

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal =
        (salesPhone != null)
            ? customerRepository.countCustomersCreatedBeforeForSales(salesPhone, startDate)
            : customerRepository.countCustomersCreatedBefore(startDate);

    for (Object[] row : results) {
      String periodStr = (String) row[0];
      Long newCustomers = ((Number) row[1]).longValue();
      runningTotal += newCustomers;

      // Parse date based on granularity
      LocalDate date;
      if (isMonthly) {
        // For monthly data (YYYY-MM format), use first day of the month
        String[] parts = periodStr.split("-");
        date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
      } else {
        // For daily data (YYYY-MM-DD format)
        date = LocalDate.parse(periodStr);
      }

      // For simplicity, use overall conversion rate - could be enhanced to calculate per-date
      BigDecimal conversionRateAtDate = calculateConversionRate(salesPhone, runningTotal);

      dataPoints.add(new TrendDataPoint(date, newCustomers, runningTotal, conversionRateAtDate));
    }

    return new TrendAnalysisResponse(dataPoints, granularity, days);
  }

  /**
   * Get customer certification trends by certificate type.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @return Certificate type trends data
   */
  public CertificateTypeTrendsResponse getCustomerTrendsByCertificateType(String salesPhone, int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    // Use monthly aggregation for certificate type trends
    List<Object[]> results =
        (salesPhone != null)
            ? customerRepository.getCustomerTrendsByCertificateTypeByMonthForSales(salesPhone, startDateStr, endDateStr)
            : customerRepository.getCustomerTrendsByCertificateTypeByMonth(startDateStr, endDateStr);

    // Group by certificate type and create trend data
    Map<String, List<TrendDataPoint>> trendsByType = new HashMap<>();
    Map<String, Long> runningTotals = new HashMap<>();

    // Initialize running totals for each certificate type
    for (Object[] row : results) {
      String certificateType;
      if (row[1] instanceof com.example.customers.model.CertificateType) {
        certificateType = ((com.example.customers.model.CertificateType) row[1]).name();
      } else {
        certificateType = (String) row[1];
      }
      runningTotals.put(certificateType, 0L);
    }

    // Process results grouped by date and certificate type
    Map<String, Map<String, Long>> dailyCountsByType = new HashMap<>();
    for (Object[] row : results) {
      String dateStr = (String) row[0];

      // Handle CertificateType enum conversion
      String certificateType;
      if (row[1] instanceof com.example.customers.model.CertificateType) {
        certificateType = ((com.example.customers.model.CertificateType) row[1]).name();
      } else {
        certificateType = (String) row[1];
      }

      Long count = ((Number) row[2]).longValue();

      dailyCountsByType
          .computeIfAbsent(dateStr, k -> new HashMap<>())
          .put(certificateType, count);
    }

    // Get all unique dates and sort them
    List<String> sortedDates = dailyCountsByType.keySet().stream()
        .sorted()
        .toList();

    // Build trend data points for each certificate type
    for (String certificateType : runningTotals.keySet()) {
      List<TrendDataPoint> typeDataPoints = new ArrayList<>();
      long runningTotal = 0;

      for (String dateStr : sortedDates) {
        Map<String, Long> dailyData = dailyCountsByType.get(dateStr);
        long dailyCount = dailyData != null && dailyData.containsKey(certificateType)
            ? dailyData.get(certificateType)
            : 0L;

        runningTotal += dailyCount;

        // Parse date based on format (YYYY-MM for monthly data)
        LocalDate date;
        if (dateStr.matches("\\d{4}-\\d{2}")) {
          // Monthly format (YYYY-MM) - use first day of the month
          String[] parts = dateStr.split("-");
          date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
        } else {
          // Daily format (YYYY-MM-DD)
          date = LocalDate.parse(dateStr);
        }

        // Use overall conversion rate for simplicity
        BigDecimal conversionRate = calculateConversionRate(salesPhone, runningTotal);
        typeDataPoints.add(new TrendDataPoint(date, dailyCount, runningTotal, conversionRate));
      }

      trendsByType.put(certificateType, typeDataPoints);
    }

    return new CertificateTypeTrendsResponse(trendsByType, days);
  }

  /**
   * Get sales performance metrics.
   *
   * @param salesPhone Sales phone for filtering (null for admin view)
   * @param days Number of days for analysis
   * @return Sales performance data
   */
  public SalesPerformanceResponse getSalesPerformance(String salesPhone, int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    long totalCustomers =
        (salesPhone != null)
            ? customerRepository.countTotalActiveCustomersBySales(salesPhone)
            : customerRepository.countTotalActiveCustomers();

    long newCustomers =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, startDateStr, endDateStr)
            : customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long conversions =
        (salesPhone != null)
            ? customerRepository.countConversionsInPeriodBySales(
                CustomerStatus.CERTIFIED, salesPhone, startDate, ZonedDateTime.now())
            : customerRepository.countConversionsInPeriod(
                CustomerStatus.CERTIFIED, startDate, ZonedDateTime.now());

    BigDecimal conversionRate = calculateConversionRate(salesPhone, totalCustomers);

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
    ZonedDateTime startDate = ZonedDateTime.now().minusDays(days);

    List<Object[]> results = salesRepository.getSalesLeaderboardData(startDate, metric);

    List<SalesPerformanceEntry> rankings = new ArrayList<>();
    int rank = 1;

    for (Object[] row : results) {
      String phone = (String) row[0];
      Long totalCustomers = ((Number) row[1]).longValue();
      Long conversions = ((Number) row[2]).longValue();
      BigDecimal conversionRate =
          new BigDecimal(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);

      rankings.add(
          new SalesPerformanceEntry(phone, totalCustomers, conversions, conversionRate, rank++));
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
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    long activeCustomersToday =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, todayStr, todayStr)
            : customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    long newCustomersToday =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, todayStr, todayStr)
            : customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    // Conversions are still based on createdAt since they represent status changes
    ZonedDateTime startOfDay = today.atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    long conversionsToday =
        (salesPhone != null)
            ? customerRepository.countConversionsInPeriodBySales(
                CustomerStatus.CERTIFIED, salesPhone, startOfDay, now)
            : customerRepository.countConversionsInPeriod(
                CustomerStatus.CERTIFIED, startOfDay, now);

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

  private BigDecimal calculateConversionRate(String salesPhone, long totalCustomers) {
    if (totalCustomers == 0) {
      return BigDecimal.ZERO;
    }

    long conversions =
        (salesPhone != null)
            ? customerRepository.countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
                CustomerStatus.CERTIFIED, salesPhone)
            : customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED);

    return BigDecimal.valueOf(conversions)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
  }

  private long getActiveCustomers(String salesPhone) {
    // Define active customers as those with recent certifications (last 30 days)
    LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
    LocalDate today = LocalDate.now();
    String thirtyDaysAgoStr = thirtyDaysAgo.toString();
    String todayStr = today.toString();

    // For now, use simplified logic - active customers are those certified recently
    // This could be enhanced with a proper StatusHistory repository method
    return (salesPhone != null)
        ? customerRepository.countNewCustomersInPeriodBySales(
            salesPhone, thirtyDaysAgoStr, todayStr)
        : customerRepository.countNewCustomersInPeriod(thirtyDaysAgoStr, todayStr);
  }

  private Map<String, Long> getStatusBreakdown(String salesPhone) {
    List<Object[]> results =
        (salesPhone != null)
            ? customerRepository.countCustomersByStatusForSales(salesPhone)
            : customerRepository.countCustomersByStatus();

    Map<String, Long> breakdown = new HashMap<>();

    for (Object[] row : results) {
      CustomerStatus status = (CustomerStatus) row[0];
      Long count = ((Number) row[1]).longValue();
      breakdown.put(status.name(), count);
    }

    return breakdown;
  }

  private PeriodChange calculatePeriodChange(
      long totalCustomers,
      long newCustomersThisPeriod,
      long newCustomersPreviousPeriod,
      BigDecimal conversionRate,
      BigDecimal previousConversionRate) {

    BigDecimal totalCustomersChange =
        calculatePercentageChange(totalCustomers - newCustomersThisPeriod, totalCustomers);
    BigDecimal newCustomersChange =
        calculatePercentageChange(newCustomersPreviousPeriod, newCustomersThisPeriod);
    BigDecimal conversionRateChange =
        previousConversionRate.compareTo(BigDecimal.ZERO) == 0
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
