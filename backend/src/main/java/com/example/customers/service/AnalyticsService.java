package com.example.customers.service;

import com.example.customers.controller.AnalyticsController.AgentPerformanceTrendsResponse;
import com.example.customers.controller.AnalyticsController.CertificateTypeTrendsResponse;
import com.example.customers.controller.AnalyticsController.DashboardOverviewResponse;
import com.example.customers.controller.AnalyticsController.LeaderboardResponse;
import com.example.customers.controller.AnalyticsController.RealtimeMetricsResponse;
import com.example.customers.controller.AnalyticsController.SalesPerformanceResponse;
import com.example.customers.controller.AnalyticsController.StatusDistributionResponse;
import com.example.customers.controller.AnalyticsController.TrendAnalysisResponse;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.StatusHistoryRepository;
import com.example.customers.service.agent.AgentPerformanceService;
import com.example.customers.service.agent.StatusChangeTrendsService;
import com.example.customers.service.dashboard.DashboardOverviewService;
import com.example.customers.service.dashboard.RealtimeMetricsService;
import com.example.customers.service.dashboard.StatusDistributionService;
import com.example.customers.service.leaderboard.LeaderboardService;
import com.example.customers.service.trends.CertificateTypeTrendsService;
import com.example.customers.service.trends.TrendAnalysisService;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

  private final DashboardOverviewService dashboardOverviewService;
  private final StatusDistributionService statusDistributionService;
  private final RealtimeMetricsService realtimeMetricsService;
  private final TrendAnalysisService trendAnalysisService;
  private final CertificateTypeTrendsService certificateTypeTrendsService;
  private final LeaderboardService leaderboardService;
  private final AgentPerformanceService agentPerformanceService;
  private final StatusChangeTrendsService statusChangeTrendsService;
  private final CustomerRepository customerRepository;
  private final SalesRepository salesRepository;
  private final StatusHistoryRepository statusHistoryRepository;
  private final MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

  @Autowired
  public AnalyticsService(
      DashboardOverviewService dashboardOverviewService,
      StatusDistributionService statusDistributionService,
      RealtimeMetricsService realtimeMetricsService,
      TrendAnalysisService trendAnalysisService,
      CertificateTypeTrendsService certificateTypeTrendsService,
      LeaderboardService leaderboardService,
      AgentPerformanceService agentPerformanceService,
      StatusChangeTrendsService statusChangeTrendsService,
      CustomerRepository customerRepository,
      SalesRepository salesRepository,
      StatusHistoryRepository statusHistoryRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository) {
    this.dashboardOverviewService = dashboardOverviewService;
    this.statusDistributionService = statusDistributionService;
    this.realtimeMetricsService = realtimeMetricsService;
    this.trendAnalysisService = trendAnalysisService;
    this.certificateTypeTrendsService = certificateTypeTrendsService;
    this.leaderboardService = leaderboardService;
    this.agentPerformanceService = agentPerformanceService;
    this.statusChangeTrendsService = statusChangeTrendsService;
    this.customerRepository = customerRepository;
    this.salesRepository = salesRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
  }

  public DashboardOverviewResponse getDashboardOverview(int days) {
    return dashboardOverviewService.getDashboardOverview(days);
  }

  public StatusDistributionResponse getStatusDistribution() {
    return statusDistributionService.getStatusDistribution();
  }

  public TrendAnalysisResponse getCustomerTrends(int days, String granularity) {
    return trendAnalysisService.getCustomerTrends(days, granularity);
  }

  public CertificateTypeTrendsResponse getCustomerTrendsByCertificateType(int days) {
    return certificateTypeTrendsService.getCustomerTrendsByCertificateType(days);
  }

  public AgentPerformanceTrendsResponse getAgentPerformanceTrends() {
    return agentPerformanceService.getAgentPerformanceTrends();
  }

  public Map<String, Object> getDailyStatusChangeTrends(int days) {
    return statusChangeTrendsService.getDailyStatusChangeTrends(days);
  }

  public SalesPerformanceResponse getSalesPerformance(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    long totalCustomers = customerRepository.countTotalActiveCustomers();

    long newCustomers = customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long conversions =
        customerRepository.countConversionsInPeriod(
            com.example.customers.model.CustomerStatus.CERTIFIED, startDate, ZonedDateTime.now());

    long unsettled = getUnsettledCustomers(totalCustomers);
    BigDecimal conversionRate = calculateConversionRate(totalCustomers, unsettled);

    Map<String, Long> statusBreakdown = getStatusBreakdown();

    return new SalesPerformanceResponse(
        "ADMIN", totalCustomers, newCustomers, conversions, conversionRate, statusBreakdown);
  }

  public LeaderboardResponse getSalesLeaderboard(int days, String metric) {
    return leaderboardService.getSalesLeaderboard(days, metric);
  }

  public LeaderboardResponse getSalesLeaderboardByYear(int year, String metric) {
    return leaderboardService.getSalesLeaderboardByYear(year, metric);
  }

  public LeaderboardResponse getSalesLeaderboardByMonth(int year, int month, String metric) {
    return leaderboardService.getSalesLeaderboardByMonth(year, month, metric);
  }

  public RealtimeMetricsResponse getRealtimeMetrics() {
    return realtimeMetricsService.getRealtimeMetrics();
  }

  @Scheduled(cron = "0 0 2 * * *")
  public void generateDailySnapshots() {}

  private BigDecimal calculateConversionRate(long totalCustomers, long unsettledCustomers) {
    if (totalCustomers == 0) {
      return BigDecimal.ZERO;
    }

    long settledCustomers = totalCustomers - unsettledCustomers;

    return BigDecimal.valueOf(settledCustomers)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, java.math.RoundingMode.HALF_UP);
  }

  private long getUnsettledCustomers(long totalCustomers) {
    return customerRepository.countNotCertifiedCustomers();
  }

  private Map<String, Long> getStatusBreakdown() {
    List<Object[]> results = customerRepository.countCustomersByStatus();

    Map<String, Long> breakdown = new HashMap<>();

    for (Object[] row : results) {
      com.example.customers.model.CustomerStatus status =
          (com.example.customers.model.CustomerStatus) row[0];
      Long count = ((Number) row[1]).longValue();
      breakdown.put(status.name(), count);
    }

    return breakdown;
  }
}
