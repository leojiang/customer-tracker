package com.example.customers.controller;

import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for analytics and dashboard operations.
 *
 * <p>Provides endpoints for dashboard metrics, charts, and analytics data with role-based access
 * control.
 */
@Tag(
    name = "Analytics & Dashboard",
    description = "APIs for dashboard metrics, charts, and analytics data")
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

  private final AnalyticsService analyticsService;
  private final CustomerRepository customerRepository;

  @Autowired
  public AnalyticsController(
      AnalyticsService analyticsService, CustomerRepository customerRepository) {
    this.analyticsService = analyticsService;
    this.customerRepository = customerRepository;
  }

  @Operation(
      summary = "Get dashboard overview metrics",
      description =
          "Retrieve key dashboard metrics including customer counts, conversion rates, and trends. "
              + "Admin users see system-wide data, Sales users see personal data only.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard overview retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardOverviewResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/dashboard/overview")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<DashboardOverviewResponse> getDashboardOverview(
      @Parameter(description = "Number of days for trend analysis", example = "30")
          @RequestParam(defaultValue = "30")
          int days) {

    String salesPhone = getCurrentUserSalesPhone();
    DashboardOverviewResponse overview = analyticsService.getDashboardOverview(salesPhone, days);
    return ResponseEntity.ok(overview);
  }

  @Operation(
      summary = "Get customer status distribution",
      description = "Retrieve current distribution of customers across all status categories")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status distribution retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/customers/status-distribution")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<StatusDistributionResponse> getStatusDistribution() {

    String salesPhone = getCurrentUserSalesPhone();
    StatusDistributionResponse distribution = analyticsService.getStatusDistribution(salesPhone);
    return ResponseEntity.ok(distribution);
  }

  @Operation(
      summary = "Get customer acquisition trends",
      description = "Retrieve customer acquisition trends over time with configurable granularity")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Customer trends retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/customers/trends")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<TrendAnalysisResponse> getCustomerTrends(
      @Parameter(description = "Number of days for analysis", example = "365")
          @RequestParam(defaultValue = "365")
          int days,
      @Parameter(description = "Data granularity: daily, weekly", example = "daily")
          @RequestParam(defaultValue = "daily")
          String granularity) {

    String salesPhone = getCurrentUserSalesPhone();
    TrendAnalysisResponse trends =
        analyticsService.getCustomerTrends(salesPhone, days, granularity);
    return ResponseEntity.ok(trends);
  }

  @Operation(
      summary = "Get customer certification trends by certificate type",
      description =
          "Retrieve customer certification trends broken down by certificate type with different colors for visualization")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Certificate type trends retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/customers/trends-by-certificate-type")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<CertificateTypeTrendsResponse> getCustomerTrendsByCertificateType(
      @Parameter(description = "Number of days for analysis", example = "90")
          @RequestParam(defaultValue = "90")
          int days) {

    String salesPhone = getCurrentUserSalesPhone();
    CertificateTypeTrendsResponse trends =
        analyticsService.getCustomerTrendsByCertificateType(salesPhone, days);
    return ResponseEntity.ok(trends);
  }

  @Operation(
      summary = "Get customer agent performance trends",
      description =
          "Retrieve monthly performance metrics for all customer agents, allowing visualization of "
              + "individual performance trends over time")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Agent performance trends retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
      })
  @GetMapping("/agents/performance-trends")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<AgentPerformanceTrendsResponse> getAgentPerformanceTrends() {

    AgentPerformanceTrendsResponse trends = analyticsService.getAgentPerformanceTrends();
    return ResponseEntity.ok(trends);
  }

  @Operation(
      summary = "Get sales performance metrics",
      description =
          "Retrieve sales performance data. Admin users see team performance, "
              + "Sales users see personal performance only.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sales performance retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/sales/performance")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<SalesPerformanceResponse> getSalesPerformance(
      @Parameter(description = "Number of days for analysis", example = "30")
          @RequestParam(defaultValue = "30")
          int days) {

    String salesPhone = getCurrentUserSalesPhone();
    SalesPerformanceResponse performance = analyticsService.getSalesPerformance(salesPhone, days);
    return ResponseEntity.ok(performance);
  }

  @Operation(
      summary = "Get sales team leaderboard (Admin only)",
      description = "Retrieve sales team performance rankings and comparisons")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
      })
  @GetMapping("/sales/leaderboard")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<LeaderboardResponse> getSalesLeaderboard(
      @Parameter(description = "Number of days for analysis", example = "30")
          @RequestParam(defaultValue = "30")
          int days,
      @Parameter(
              description = "Ranking metric: customers, conversions, rate",
              example = "conversions")
          @RequestParam(defaultValue = "conversions")
          String metric) {

    LeaderboardResponse leaderboard = analyticsService.getSalesLeaderboard(days, metric);
    return ResponseEntity.ok(leaderboard);
  }

  @Operation(
      summary = "Get sales team leaderboard by year (Admin only)",
      description = "Retrieve sales team performance rankings for a specific year (all months)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Yearly leaderboard retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
      })
  @GetMapping("/sales/leaderboard/yearly")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<LeaderboardResponse> getSalesLeaderboardByYear(
      @Parameter(description = "Year for filtering (e.g., 2026)", example = "2026") @RequestParam
          int year,
      @Parameter(
              description = "Ranking metric: customers, conversions, rate",
              example = "conversions")
          @RequestParam(defaultValue = "conversions")
          String metric) {

    LeaderboardResponse leaderboard = analyticsService.getSalesLeaderboardByYear(year, metric);
    return ResponseEntity.ok(leaderboard);
  }

  @Operation(
      summary = "Get sales team leaderboard by month (Admin only)",
      description = "Retrieve sales team performance rankings for a specific month")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Monthly leaderboard retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
      })
  @GetMapping("/sales/leaderboard/monthly")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<LeaderboardResponse> getSalesLeaderboardByMonth(
      @Parameter(description = "Year for filtering (e.g., 2026)", example = "2026") @RequestParam
          int year,
      @Parameter(description = "Month for filtering (1-12)", example = "1") @RequestParam int month,
      @Parameter(
              description = "Ranking metric: customers, conversions, rate",
              example = "conversions")
          @RequestParam(defaultValue = "conversions")
          String metric) {

    LeaderboardResponse leaderboard =
        analyticsService.getSalesLeaderboardByMonth(year, month, metric);
    return ResponseEntity.ok(leaderboard);
  }

  @Operation(
      summary = "Get real-time metrics",
      description = "Retrieve current real-time dashboard metrics and indicators")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Real-time metrics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping("/realtime/metrics")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<RealtimeMetricsResponse> getRealtimeMetrics() {

    String salesPhone = getCurrentUserSalesPhone();
    RealtimeMetricsResponse metrics = analyticsService.getRealtimeMetrics(salesPhone);
    return ResponseEntity.ok(metrics);
  }

  @Operation(
      summary = "Debug endpoint to check certified dates in database",
      description = "Returns sample of certified_at dates to understand data distribution")
  @GetMapping("/debug/certified-dates")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Map<String, Object>> getCertifiedDatesDebug() {
    Map<String, Object> debug = new java.util.HashMap<>();

    // Get sample dates from database
    java.util.List<String> sampleDates = customerRepository.findSampleCertifiedDates();
    debug.put("sampleCertifiedDates", sampleDates);
    debug.put("sampleCount", sampleDates.size());

    // Get date range
    String minDate = customerRepository.findMinCertifiedDate();
    String maxDate = customerRepository.findMaxCertifiedDate();
    debug.put("minDate", minDate);
    debug.put("maxDate", maxDate);

    return ResponseEntity.ok(debug);
  }

  // Helper methods for authorization
  private Sales getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Sales) {
      return (Sales) authentication.getPrincipal();
    }
    throw new IllegalStateException("No authenticated user found");
  }

  private String getCurrentUserSalesPhone() {
    Sales currentUser = getCurrentUser();
    // Admin can see all data (no filter), regular sales can only see their own
    return currentUser.getRole() == SalesRole.ADMIN ? null : currentUser.getPhone();
  }

  // Response DTOs
  /** Response DTO for dashboard overview metrics. */
  public static class DashboardOverviewResponse {
    private long totalCustomers;
    private long newCustomersThisPeriod;
    private long unsettledCustomers;
    private BigDecimal conversionRate;
    private PeriodChange periodChange;

    public DashboardOverviewResponse(
        long totalCustomers,
        long newCustomersThisPeriod,
        long unsettledCustomers,
        BigDecimal conversionRate,
        PeriodChange periodChange) {
      this.totalCustomers = totalCustomers;
      this.newCustomersThisPeriod = newCustomersThisPeriod;
      this.unsettledCustomers = unsettledCustomers;
      this.conversionRate = conversionRate;
      this.periodChange = periodChange;
    }

    // Getters
    public long getTotalCustomers() {
      return totalCustomers;
    }

    public long getNewCustomersThisPeriod() {
      return newCustomersThisPeriod;
    }

    public long getUnsettledCustomers() {
      return unsettledCustomers;
    }

    public BigDecimal getConversionRate() {
      return conversionRate;
    }

    public PeriodChange getPeriodChange() {
      return periodChange;
    }
  }

  /** DTO for period-over-period change metrics. */
  public static class PeriodChange {
    private BigDecimal totalCustomersChange;
    private BigDecimal newCustomersChange;
    private BigDecimal conversionRateChange;

    public PeriodChange(
        BigDecimal totalCustomersChange,
        BigDecimal newCustomersChange,
        BigDecimal conversionRateChange) {
      this.totalCustomersChange = totalCustomersChange;
      this.newCustomersChange = newCustomersChange;
      this.conversionRateChange = conversionRateChange;
    }

    // Getters
    public BigDecimal getTotalCustomersChange() {
      return totalCustomersChange;
    }

    public BigDecimal getNewCustomersChange() {
      return newCustomersChange;
    }

    public BigDecimal getConversionRateChange() {
      return conversionRateChange;
    }
  }

  /** Response DTO for customer status distribution. */
  public static class StatusDistributionResponse {
    private Map<String, Long> statusCounts;
    private long totalCustomers;

    public StatusDistributionResponse(Map<String, Long> statusCounts, long totalCustomers) {
      this.statusCounts = statusCounts;
      this.totalCustomers = totalCustomers;
    }

    public Map<String, Long> getStatusCounts() {
      return statusCounts;
    }

    public long getTotalCustomers() {
      return totalCustomers;
    }
  }

  /** Response DTO for trend analysis data. */
  public static class TrendAnalysisResponse {
    private List<TrendDataPoint> dataPoints;
    private String granularity;
    private int totalDays;

    public TrendAnalysisResponse(
        List<TrendDataPoint> dataPoints, String granularity, int totalDays) {
      this.dataPoints = dataPoints;
      this.granularity = granularity;
      this.totalDays = totalDays;
    }

    public List<TrendDataPoint> getDataPoints() {
      return dataPoints;
    }

    public String getGranularity() {
      return granularity;
    }

    public int getTotalDays() {
      return totalDays;
    }
  }

  /** DTO for individual trend data points. */
  public static class TrendDataPoint {
    private String date;
    private long newCustomers;
    private long totalCustomers;
    private BigDecimal conversionRate;

    public TrendDataPoint(
        LocalDate date, long newCustomers, long totalCustomers, BigDecimal conversionRate) {
      // Format: if date is first day of month, use YYYY-MM format, otherwise YYYY-MM-DD
      if (date.getDayOfMonth() == 1) {
        this.date = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
      } else {
        this.date = date.toString();
      }
      this.newCustomers = newCustomers;
      this.totalCustomers = totalCustomers;
      this.conversionRate = conversionRate;
    }

    // Getters
    public String getDate() {
      return date;
    }

    public long getNewCustomers() {
      return newCustomers;
    }

    public long getTotalCustomers() {
      return totalCustomers;
    }

    public BigDecimal getConversionRate() {
      return conversionRate;
    }
  }

  /** Response DTO for sales performance metrics. */
  public static class SalesPerformanceResponse {
    private String salesPhone;
    private long totalCustomers;
    private long newCustomers;
    private long conversions;
    private BigDecimal conversionRate;
    private Map<String, Long> statusBreakdown;

    public SalesPerformanceResponse(
        String salesPhone,
        long totalCustomers,
        long newCustomers,
        long conversions,
        BigDecimal conversionRate,
        Map<String, Long> statusBreakdown) {
      this.salesPhone = salesPhone;
      this.totalCustomers = totalCustomers;
      this.newCustomers = newCustomers;
      this.conversions = conversions;
      this.conversionRate = conversionRate;
      this.statusBreakdown = statusBreakdown;
    }

    // Getters
    public String getSalesPhone() {
      return salesPhone;
    }

    public long getTotalCustomers() {
      return totalCustomers;
    }

    public long getNewCustomers() {
      return newCustomers;
    }

    public long getConversions() {
      return conversions;
    }

    public BigDecimal getConversionRate() {
      return conversionRate;
    }

    public Map<String, Long> getStatusBreakdown() {
      return statusBreakdown;
    }
  }

  /** Response DTO for sales team leaderboard. */
  public static class LeaderboardResponse {
    private List<SalesPerformanceEntry> rankings;
    private int totalDays;
    private String metric;

    public LeaderboardResponse(List<SalesPerformanceEntry> rankings, int totalDays, String metric) {
      this.rankings = rankings;
      this.totalDays = totalDays;
      this.metric = metric;
    }

    public List<SalesPerformanceEntry> getRankings() {
      return rankings;
    }

    public int getTotalDays() {
      return totalDays;
    }

    public String getMetric() {
      return metric;
    }
  }

  /** DTO for individual sales performance entries. */
  public static class SalesPerformanceEntry {
    private String salesPhone;
    private long totalCustomers;
    private long conversions;
    private BigDecimal conversionRate;
    private int rank;

    public SalesPerformanceEntry(
        String salesPhone,
        long totalCustomers,
        long conversions,
        BigDecimal conversionRate,
        int rank) {
      this.salesPhone = salesPhone;
      this.totalCustomers = totalCustomers;
      this.conversions = conversions;
      this.conversionRate = conversionRate;
      this.rank = rank;
    }

    // Getters
    public String getSalesPhone() {
      return salesPhone;
    }

    public long getTotalCustomers() {
      return totalCustomers;
    }

    public long getConversions() {
      return conversions;
    }

    public BigDecimal getConversionRate() {
      return conversionRate;
    }

    public int getRank() {
      return rank;
    }
  }

  /** Response DTO for real-time metrics. */
  public static class RealtimeMetricsResponse {
    private long unsettledCustomersToday;
    private long newCustomersToday;
    private long conversionsToday;
    private String lastUpdated;

    public RealtimeMetricsResponse(
        long unsettledCustomersToday,
        long newCustomersToday,
        long conversionsToday,
        String lastUpdated) {
      this.unsettledCustomersToday = unsettledCustomersToday;
      this.newCustomersToday = newCustomersToday;
      this.conversionsToday = conversionsToday;
      this.lastUpdated = lastUpdated;
    }

    // Getters
    public long getUnsettledCustomersToday() {
      return unsettledCustomersToday;
    }

    public long getNewCustomersToday() {
      return newCustomersToday;
    }

    public long getConversionsToday() {
      return conversionsToday;
    }

    public String getLastUpdated() {
      return lastUpdated;
    }
  }

  /** Response DTO for certificate type trends analysis. */
  public static class CertificateTypeTrendsResponse {
    private java.util.Map<String, List<TrendDataPoint>> trendsByCertificateType;
    private int totalDays;

    public CertificateTypeTrendsResponse(
        java.util.Map<String, List<TrendDataPoint>> trendsByCertificateType, int totalDays) {
      this.trendsByCertificateType = trendsByCertificateType;
      this.totalDays = totalDays;
    }

    public java.util.Map<String, List<TrendDataPoint>> getTrendsByCertificateType() {
      return trendsByCertificateType;
    }

    public int getTotalDays() {
      return totalDays;
    }
  }

  /** Response DTO for customer agent performance trends analysis. */
  public static class AgentPerformanceTrendsResponse {
    private java.util.Map<String, List<TrendDataPoint>> trendsByAgent;
    private List<String> agents;

    public AgentPerformanceTrendsResponse(
        java.util.Map<String, List<TrendDataPoint>> trendsByAgent, List<String> agents) {
      this.trendsByAgent = trendsByAgent;
      this.agents = agents;
    }

    public java.util.Map<String, List<TrendDataPoint>> getTrendsByAgent() {
      return trendsByAgent;
    }

    public List<String> getAgents() {
      return agents;
    }
  }
}
