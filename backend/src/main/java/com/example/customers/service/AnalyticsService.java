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
  private final com.example.customers.repository.MonthlyAgentPerformanceRepository
      monthlyAgentPerformanceRepository;

  @Autowired
  public AnalyticsService(
      CustomerRepository customerRepository,
      SalesRepository salesRepository,
      StatusHistoryRepository statusHistoryRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository,
      MonthlyCertifiedCountByCertificateTypeRepository
          monthlyCertifiedCountByCertificateTypeRepository,
      com.example.customers.repository.MonthlyAgentPerformanceRepository
          monthlyAgentPerformanceRepository) {
    this.customerRepository = customerRepository;
    this.salesRepository = salesRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
    this.monthlyCertifiedCountByCertificateTypeRepository =
        monthlyCertifiedCountByCertificateTypeRepository;
    this.monthlyAgentPerformanceRepository = monthlyAgentPerformanceRepository;
  }

  /**
   * Get dashboard overview metrics.
   *
   * @param days Number of days for analysis
   * @return Dashboard overview data
   */
  public DashboardOverviewResponse getDashboardOverview(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);
    ZonedDateTime previousStartDate = startDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();
    String previousStartDateStr = previousStartDate.toLocalDate().toString();
    String startDateComparisonStr = startDate.toLocalDate().toString();

    // Current period metrics
    // Use the certified count table to get total customers (sum of all monthly certifications)
    List<com.example.customers.entity.MonthlyCertifiedCount> monthlyCounts =
        monthlyCertifiedCountRepository.findAllByOrderByMonthAsc();
    long totalCustomers =
        monthlyCounts.stream().mapToLong(mc -> mc.getCertifiedCount().longValue()).sum();

    long newCustomersThisPeriod =
        customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long unsettledCustomers = getUnsettledCustomers(totalCustomers);
    BigDecimal conversionRate = calculateConversionRate(totalCustomers, unsettledCustomers);

    // Previous period metrics for comparison
    long newCustomersPreviousPeriod =
        customerRepository.countNewCustomersInPeriod(previousStartDateStr, startDateComparisonStr);

    long previousTotalCustomers = totalCustomers - newCustomersThisPeriod;
    // For previous period, we need to calculate unsettled customers as well
    // Using the same ratio as current period for simplicity
    long previousUnsettledCustomers = getUnsettledCustomers(previousTotalCustomers);
    BigDecimal previousConversionRate =
        calculateConversionRate(previousTotalCustomers, previousUnsettledCustomers);

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
   * @return Status distribution data
   */
  public StatusDistributionResponse getStatusDistribution() {
    List<Object[]> results = customerRepository.countCustomersByStatus();

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
   * @param days Number of days for analysis
   * @param granularity Data granularity (daily, weekly)
   * @return Trend analysis data
   */
  public TrendAnalysisResponse getCustomerTrends(int days, String granularity) {
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);

    // Use the new monthly_certified_count table for monthly trends (much simpler and faster)
    if (isMonthly) {
      return getMonthlyTrendsFromNewTable();
    }

    // For daily granularity, use the existing complex queries
    return getTrendsFromCustomerTable(days, granularity);
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
      Long newCustomerCertifiedCount =
          monthlyCount.getNewCustomerCertifiedCount() != null
              ? monthlyCount.getNewCustomerCertifiedCount().longValue()
              : 0L;
      Long renewCustomerCertifiedCount =
          monthlyCount.getRenewCustomerCertifiedCount() != null
              ? monthlyCount.getRenewCustomerCertifiedCount().longValue()
              : 0L;
      runningTotal += newCertifications;

      // Parse month (YYYY-MM) to date (first day of month)
      String[] parts = monthStr.split("-");
      LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

      // Calculate conversion rate
      long unsettledAtDate = getUnsettledCustomers(runningTotal);
      BigDecimal conversionRateAtDate = calculateConversionRate(runningTotal, unsettledAtDate);

      dataPoints.add(
          new TrendDataPoint(
              date,
              newCertifications,
              runningTotal,
              conversionRateAtDate,
              newCustomerCertifiedCount,
              renewCustomerCertifiedCount));
    }

    return new TrendAnalysisResponse(dataPoints, "monthly", 0);
  }

  /**
   * Get daily status change trends for the 4 key statuses.
   *
   * <p>Returns counts of status changes per day, using only the latest status per customer per day.
   *
   * @param days Number of days to analyze
   * @return Status change trends data
   */
  public Map<String, Object> getDailyStatusChangeTrends(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    List<Object[]> results =
        statusHistoryRepository.findDailyStatusChangeTrendsLatestPerCustomer(startDate, endDate);

    // Process results into a structured format
    Map<LocalDate, Map<String, Integer>> dailyStatusCounts = new HashMap<>();

    for (Object[] row : results) {
      // Convert java.sql.Date to LocalDate
      LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
      CustomerStatus status = (CustomerStatus) row[1];
      Long count = ((Number) row[2]).longValue();

      dailyStatusCounts
          .computeIfAbsent(date, k -> new HashMap<>())
          .put(status.name(), count.intValue());
    }

    // Build response data structure
    List<Map<String, Object>> dataPoints = new ArrayList<>();
    for (LocalDate date = startDate.toLocalDate();
         !date.isAfter(endDate.toLocalDate());
         date = date.plusDays(1)) {
      Map<String, Object> dataPoint = new HashMap<>();
      dataPoint.put("date", date.toString());

      Map<String, Integer> statusCounts = dailyStatusCounts.getOrDefault(date, new HashMap<>());
      dataPoint.put("NOTIFIED", statusCounts.getOrDefault("NOTIFIED", 0));
      dataPoint.put("SUBMITTED", statusCounts.getOrDefault("SUBMITTED", 0));
      dataPoint.put("ABORTED", statusCounts.getOrDefault("ABORTED", 0));
      dataPoint.put("CERTIFIED_ELSEWHERE", statusCounts.getOrDefault("CERTIFIED_ELSEWHERE", 0));

      dataPoints.add(dataPoint);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("dataPoints", dataPoints);
    response.put("granularity", "daily");
    response.put("totalDays", days);

    return response;
  }

  /** Get trends from the customers table (complex query, used for daily granularity). */
  private TrendAnalysisResponse getTrendsFromCustomerTable(int days, String granularity) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    // Choose query based on granularity
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);
    List<Object[]> results;

    if (isMonthly) {
      results = customerRepository.getCustomerTrendsByMonth(startDateStr, endDateStr);
    } else {
      results = customerRepository.getCustomerTrendsByDate(startDateStr, endDateStr);
    }

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal = customerRepository.countCustomersCreatedBefore(startDate);

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
      long unsettledAtDate = getUnsettledCustomers(runningTotal);
      BigDecimal conversionRateAtDate = calculateConversionRate(runningTotal, unsettledAtDate);

      // Note: Daily granularity doesn't have customer type breakdown, so we pass 0
      dataPoints.add(
          new TrendDataPoint(date, newCustomers, runningTotal, conversionRateAtDate, 0, 0));
    }

    return new TrendAnalysisResponse(dataPoints, granularity, days);
  }

  /**
   * Get customer certification trends by certificate type.
   *
   * @param days Number of days for analysis (kept for API compatibility but not used)
   * @return Certificate type trends data
   */
  public CertificateTypeTrendsResponse getCustomerTrendsByCertificateType(int days) {
    // Use the monthly_certified_count_by_certificate_type table (simplified and fast)
    // Note: The days parameter is kept for API compatibility but not used
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

        long unsettled = getUnsettledCustomers(runningTotal);
        BigDecimal conversionRate = calculateConversionRate(runningTotal, unsettled);
        // Note: Certificate type trends don't have customer type breakdown, so we pass 0
        dataPoints.add(
            new TrendDataPoint(date, newCertifications, runningTotal, conversionRate, 0, 0));
      }

      trendsByType.put(certificateType, dataPoints);
    }

    return new CertificateTypeTrendsResponse(trendsByType, 0);
  }

  /**
   * Get sales performance metrics.
   *
   * @param days Number of days for analysis
   * @return Sales performance data
   */
  public SalesPerformanceResponse getSalesPerformance(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    // Convert dates to String format for certifiedAt queries (YYYY-MM-DD)
    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    long totalCustomers = customerRepository.countTotalActiveCustomers();

    long newCustomers = customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long conversions =
        customerRepository.countConversionsInPeriod(
            CustomerStatus.CERTIFIED, startDate, ZonedDateTime.now());

    long unsettled = getUnsettledCustomers(totalCustomers);
    BigDecimal conversionRate = calculateConversionRate(totalCustomers, unsettled);

    // Get status breakdown
    Map<String, Long> statusBreakdown = getStatusBreakdown();

    return new SalesPerformanceResponse(
        "ADMIN", totalCustomers, newCustomers, conversions, conversionRate, statusBreakdown);
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
   * @return Real-time metrics data
   */
  public RealtimeMetricsResponse getRealtimeMetrics() {
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    long unsettledCustomersToday = customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    long newCustomersToday = customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    // Conversions are still based on createdAt since they represent status changes
    ZonedDateTime startOfDay = today.atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    long conversionsToday =
        customerRepository.countConversionsInPeriod(CustomerStatus.CERTIFIED, startOfDay, now);

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

  /**
   * Get customer agent performance trends over time.
   *
   * <p>Retrieves monthly performance metrics for all customer agents, allowing visualization of
   * individual performance trends.
   *
   * @return Agent performance trends data grouped by agent
   */
  public com.example.customers.controller.AnalyticsController.AgentPerformanceTrendsResponse
      getAgentPerformanceTrends() {

    List<com.example.customers.entity.MonthlyAgentPerformance> monthlyData =
        monthlyAgentPerformanceRepository.findAllByOrderByMonthAscCustomerAgentAsc();

    // Group by customer agent
    Map<String, List<TrendDataPoint>> trendsByAgent = new HashMap<>();

    for (com.example.customers.entity.MonthlyAgentPerformance record : monthlyData) {
      String agent = record.getCustomerAgent();
      String monthStr = record.getMonth();

      // Parse month (YYYY-MM) to date (first day of month)
      String[] parts = monthStr.split("-");
      LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

      // Create trend data point for this month's data
      Long newCustomers = record.getNewCustomers().longValue();
      Long conversions = record.getConversions().longValue();
      BigDecimal conversionRate = record.getConversionRate();

      // For total customers, we use the total_customers field directly since this is a snapshot
      Long totalCustomers = record.getTotalCustomers().longValue();

      // Note: Agent performance trends don't have customer type breakdown, so we pass 0
      TrendDataPoint dataPoint =
          new TrendDataPoint(date, newCustomers, totalCustomers, conversionRate, 0, 0);

      trendsByAgent.computeIfAbsent(agent, k -> new ArrayList<>()).add(dataPoint);
    }

    // Get list of all agents
    List<String> agents = new ArrayList<>(trendsByAgent.keySet());
    java.util.Collections.sort(agents);

    return new com.example.customers.controller.AnalyticsController.AgentPerformanceTrendsResponse(
        trendsByAgent, agents);
  }

  // Helper methods

  private BigDecimal calculateConversionRate(long totalCustomers, long unsettledCustomers) {
    if (totalCustomers == 0) {
      return BigDecimal.ZERO;
    }

    // Conversion rate = (total customers - unsettled customers) / total customers * 100
    long settledCustomers = totalCustomers - unsettledCustomers;

    return BigDecimal.valueOf(settledCustomers)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
  }

  private long getUnsettledCustomers(long totalCustomers) {
    // Count customers currently NOT in CERTIFIED status (unsettled customers)
    return customerRepository.countNotCertifiedCustomers();
  }

  private Map<String, Long> getStatusBreakdown() {
    List<Object[]> results = customerRepository.countCustomersByStatus();

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
