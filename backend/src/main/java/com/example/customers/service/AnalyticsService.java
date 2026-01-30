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
import com.example.customers.entity.MonthlyCertifiedCountByCertificateType;
import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyCertifiedCountByCertificateTypeRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.StatusHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private final MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;
  private final MonthlyCertifiedCountByCertificateTypeRepository
      monthlyCertifiedCountByCertificateTypeRepository;

  @Autowired
  public AnalyticsService(
      CustomerRepository customerRepository,
      SalesRepository salesRepository,
      StatusHistoryRepository statusHistoryRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository,
      MonthlyCertifiedCountByCertificateTypeRepository
          monthlyCertifiedCountByCertificateTypeRepository) {
    this.customerRepository = customerRepository;
    this.salesRepository = salesRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
    this.monthlyCertifiedCountByCertificateTypeRepository =
        monthlyCertifiedCountByCertificateTypeRepository;
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
    // For admin users, use the certified count table to get total customers (sum of all monthly
    // certifications)
    long totalCustomers;
    if (salesPhone == null) {
      // Admin: get total from certified count table
      List<com.example.customers.entity.MonthlyCertifiedCount> monthlyCounts =
          monthlyCertifiedCountRepository.findAllByOrderByMonthAsc();
      totalCustomers =
          monthlyCounts.stream().mapToLong(mc -> mc.getCertifiedCount().longValue()).sum();
    } else {
      // Sales user: use the customer table
      totalCustomers = customerRepository.countTotalActiveCustomersBySales(salesPhone);
    }

    long newCustomersThisPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, startDateStr, endDateStr)
            : customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long unsettledCustomers = getUnsettledCustomers(salesPhone, totalCustomers);
    BigDecimal conversionRate = calculateConversionRate(salesPhone, totalCustomers);

    // Previous period metrics for comparison
    long newCustomersPreviousPeriod =
        (salesPhone != null)
            ? customerRepository.countNewCustomersInPeriodBySales(
                salesPhone, previousStartDateStr, startDateComparisonStr)
            : customerRepository.countNewCustomersInPeriod(
                previousStartDateStr, startDateComparisonStr);

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
        totalCustomers, newCustomersThisPeriod, unsettledCustomers, conversionRate, periodChange);
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
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);

    // Use the new monthly_certified_count table for admin monthly trends (much simpler and faster)
    if (isMonthly && salesPhone == null) {
      return getMonthlyTrendsFromNewTable();
    }

    // For sales users or daily granularity, use the existing complex queries
    return getTrendsFromCustomerTable(salesPhone, days, granularity);
  }

  /** Get monthly trends from the monthly_certified_count table (simplified, fast query). */
  private TrendAnalysisResponse getMonthlyTrendsFromNewTable() {
    List<com.example.customers.entity.MonthlyCertifiedCount> monthlyCounts =
        monthlyCertifiedCountRepository.findAllByOrderByMonthAsc();

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal = 0;

    for (com.example.customers.entity.MonthlyCertifiedCount monthlyCount : monthlyCounts) {
      String monthStr = monthlyCount.getMonth();
      Long newCertifications = monthlyCount.getCertifiedCount().longValue();
      runningTotal += newCertifications;

      // Parse month (YYYY-MM) to date (first day of month)
      String[] parts = monthStr.split("-");
      LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

      // Calculate conversion rate
      BigDecimal conversionRateAtDate = calculateConversionRate(null, runningTotal);

      dataPoints.add(
          new TrendDataPoint(date, newCertifications, runningTotal, conversionRateAtDate));
    }

    return new TrendAnalysisResponse(dataPoints, "monthly", 0);
  }

  /**
   * Get trends from the customers table (complex query, used for sales users or daily granularity).
   */
  private TrendAnalysisResponse getTrendsFromCustomerTable(
      String salesPhone, int days, String granularity) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    // Choose query based on granularity
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);
    List<Object[]> results;

    if (isMonthly) {
      results =
          (salesPhone != null)
              ? customerRepository.getCustomerTrendsByMonthForSales(
                  salesPhone, startDateStr, endDateStr)
              : customerRepository.getCustomerTrendsByMonth(startDateStr, endDateStr);
    } else {
      results =
          (salesPhone != null)
              ? customerRepository.getCustomerTrendsByDateForSales(
                  salesPhone, startDateStr, endDateStr)
              : customerRepository.getCustomerTrendsByDate(startDateStr, endDateStr);
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
  public CertificateTypeTrendsResponse getCustomerTrendsByCertificateType(
      String salesPhone, int days) {
    // Use the monthly_certified_count_by_certificate_type table for all users (simplified and fast)
    // Note: The salesPhone and days parameters are kept for API compatibility but not used
    return getCertificateTypeTrendsFromNewTable();
  }

  /**
   * Get certificate type trends from the monthly_certified_count_by_certificate_type table
   * (simplified, fast query).
   */
  private CertificateTypeTrendsResponse getCertificateTypeTrendsFromNewTable() {
    List<MonthlyCertifiedCountByCertificateType> monthlyCounts =
        monthlyCertifiedCountByCertificateTypeRepository
            .findAllByOrderByMonthAscCertificateTypeAsc();

    Map<String, List<TrendDataPoint>> trendsByType = new HashMap<>();
    Map<String, Long> runningTotals = new HashMap<>();

    // Group by certificate type
    Map<String, List<MonthlyCertifiedCountByCertificateType>> groupedByType = new HashMap<>();
    for (MonthlyCertifiedCountByCertificateType count : monthlyCounts) {
      String certType = count.getCertificateType();
      groupedByType.computeIfAbsent(certType, k -> new ArrayList<>()).add(count);
      runningTotals.put(certType, 0L);
    }

    // Build trend data for each certificate type
    for (Map.Entry<String, List<MonthlyCertifiedCountByCertificateType>> entry :
        groupedByType.entrySet()) {
      String certificateType = entry.getKey();
      List<MonthlyCertifiedCountByCertificateType> typeCounts = entry.getValue();
      List<TrendDataPoint> dataPoints = new ArrayList<>();
      long runningTotal = 0;

      for (MonthlyCertifiedCountByCertificateType count : typeCounts) {
        String monthStr = count.getMonth();
        Long newCertifications = count.getCertifiedCount().longValue();
        runningTotal += newCertifications;

        // Parse month (YYYY-MM) to date (first day of month)
        String[] parts = monthStr.split("-");
        LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

        BigDecimal conversionRate = calculateConversionRate(null, runningTotal);
        dataPoints.add(new TrendDataPoint(date, newCertifications, runningTotal, conversionRate));
      }

      trendsByType.put(certificateType, dataPoints);
    }

    return new CertificateTypeTrendsResponse(trendsByType, 0);
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
      String customerAgent = (String) row[0];
      Long totalCustomers = ((Number) row[1]).longValue();
      Long conversions = ((Number) row[2]).longValue();
      BigDecimal conversionRate =
          new BigDecimal(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);

      rankings.add(
          new SalesPerformanceEntry(
              customerAgent, totalCustomers, conversions, conversionRate, rank++));
    }

    return new LeaderboardResponse(rankings, days, metric);
  }

  /**
   * Get sales team leaderboard for a specific year (Admin only).
   *
   * @param year Year for filtering (e.g., 2026)
   * @param metric Ranking metric (customers, conversions, rate)
   * @return Leaderboard data
   */
  public LeaderboardResponse getSalesLeaderboardByYear(int year, String metric) {
    // Convert int year to string
    String yearStr = String.valueOf(year);

    List<Object[]> results = salesRepository.getSalesLeaderboardDataByYear(yearStr, metric);

    List<SalesPerformanceEntry> rankings = new ArrayList<>();
    int rank = 1;

    for (Object[] row : results) {
      String customerAgent = (String) row[0];
      Long totalCustomers = ((Number) row[1]).longValue();
      Long conversions = ((Number) row[2]).longValue();
      BigDecimal conversionRate =
          new BigDecimal(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);

      rankings.add(
          new SalesPerformanceEntry(
              customerAgent, totalCustomers, conversions, conversionRate, rank++));
    }

    return new LeaderboardResponse(rankings, 0, metric);
  }

  /**
   * Get sales team leaderboard for a specific month (Admin only).
   *
   * @param year Year for filtering (e.g., 2026)
   * @param month Month for filtering (1-12)
   * @param metric Ranking metric (customers, conversions, rate)
   * @return Leaderboard data
   */
  public LeaderboardResponse getSalesLeaderboardByMonth(int year, int month, String metric) {
    // Convert int year/month to strings with proper formatting
    String yearStr = String.valueOf(year);
    String monthStr = String.valueOf(month);
    if (month < 10) {
      monthStr = "0" + monthStr; // Pad single digit months with leading zero
    }

    List<Object[]> results =
        salesRepository.getSalesLeaderboardDataByMonth(yearStr, monthStr, metric);

    List<SalesPerformanceEntry> rankings = new ArrayList<>();
    int rank = 1;

    for (Object[] row : results) {
      String customerAgent = (String) row[0];
      Long totalCustomers = ((Number) row[1]).longValue();
      Long conversions = ((Number) row[2]).longValue();
      BigDecimal conversionRate =
          new BigDecimal(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);

      rankings.add(
          new SalesPerformanceEntry(
              customerAgent, totalCustomers, conversions, conversionRate, rank++));
    }

    return new LeaderboardResponse(rankings, 0, metric);
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

    long unsettledCustomersToday =
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
        unsettledCustomersToday, newCustomersToday, conversionsToday, lastUpdated);
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

  private long getUnsettledCustomers(String salesPhone, long totalCustomers) {
    // Get customers NOT in CERTIFIED status
    // (i.e., customers still in process: NEW, NOTIFIED, ABORTED, SUBMITTED, CERTIFIED_ELSEWHERE)
    long certifiedCustomers =
        (salesPhone != null)
            ? customerRepository.countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
                CustomerStatus.CERTIFIED, salesPhone)
            : customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED);

    long unsettledCustomers = totalCustomers - certifiedCustomers;

    return unsettledCustomers;
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
