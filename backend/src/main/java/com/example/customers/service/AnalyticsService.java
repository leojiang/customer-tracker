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

    // Current period metrics
    long totalCustomers =
        (salesPhone != null)
            ? customerRepository.countTotalActiveCustomersBySales(salesPhone)
            : customerRepository.countTotalActiveCustomers();

    long newCustomersThisPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, startDate, endDate)
            : customerRepository.countNewCustomersInPeriod(startDate, endDate);

    long activeCustomers = getActiveCustomers(salesPhone);
    BigDecimal conversionRate = calculateConversionRate(salesPhone, totalCustomers);

    // Previous period metrics for comparison
    long newCustomersPreviousPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, previousStartDate, startDate)
            : customerRepository.countNewCustomersInPeriod(previousStartDate, startDate);

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

    List<Object[]> results =
        (salesPhone != null)
            ? customerRepository.getCustomerTrendsByDateForSales(salesPhone, startDate, endDate)
            : customerRepository.getCustomerTrendsByDate(startDate, endDate);

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal =
        (salesPhone != null)
            ? customerRepository.countCustomersCreatedBeforeForSales(salesPhone, startDate)
            : customerRepository.countCustomersCreatedBefore(startDate);

    for (Object[] row : results) {
      LocalDate date = ((Date) row[0]).toLocalDate();
      Long newCustomers = ((Number) row[1]).longValue();
      runningTotal += newCustomers;

      // For simplicity, use overall conversion rate - could be enhanced to calculate per-date
      BigDecimal conversionRateAtDate = calculateConversionRate(salesPhone, runningTotal);

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
    ZonedDateTime startDate = ZonedDateTime.now().minusDays(days);

    long totalCustomers =
        (salesPhone != null)
            ? customerRepository.countTotalActiveCustomersBySales(salesPhone)
            : customerRepository.countTotalActiveCustomers();

    long newCustomers =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, startDate, ZonedDateTime.now())
            : customerRepository.countNewCustomersInPeriod(startDate, ZonedDateTime.now());

    long conversions =
        (salesPhone != null)
            ? customerRepository.countConversionsInPeriodBySales(
                CustomerStatus.BUSINESS_DONE, salesPhone, startDate, ZonedDateTime.now())
            : customerRepository.countConversionsInPeriod(
                CustomerStatus.BUSINESS_DONE, startDate, ZonedDateTime.now());

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
    ZonedDateTime startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    long activeCustomersToday =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, startOfDay, now)
            : customerRepository.countNewCustomersInPeriod(startOfDay, now);

    long newCustomersToday =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(salesPhone, startOfDay, now)
            : customerRepository.countNewCustomersInPeriod(startOfDay, now);

    long conversionsToday =
        (salesPhone != null)
            ? customerRepository.countConversionsInPeriodBySales(
                CustomerStatus.BUSINESS_DONE, salesPhone, startOfDay, now)
            : customerRepository.countConversionsInPeriod(
                CustomerStatus.BUSINESS_DONE, startOfDay, now);

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
                CustomerStatus.BUSINESS_DONE, salesPhone)
            : customerRepository.countByCurrentStatusAndDeletedAtIsNull(
                CustomerStatus.BUSINESS_DONE);

    return BigDecimal.valueOf(conversions)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
  }

  private long getActiveCustomers(String salesPhone) {
    // Define active customers as those with recent status changes (last 30 days)
    ZonedDateTime thirtyDaysAgo = ZonedDateTime.now().minusDays(30);

    // For now, use simplified logic - active customers are those created recently
    // This could be enhanced with a proper StatusHistory repository method
    return (salesPhone != null)
        ? customerRepository.countNewCustomersInPeriodBySales(
            salesPhone, thirtyDaysAgo, ZonedDateTime.now())
        : customerRepository.countNewCustomersInPeriod(thirtyDaysAgo, ZonedDateTime.now());
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
