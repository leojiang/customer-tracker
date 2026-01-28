package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.customers.controller.AnalyticsController.DashboardOverviewResponse;
import com.example.customers.controller.AnalyticsController.LeaderboardResponse;
import com.example.customers.controller.AnalyticsController.PeriodChange;
import com.example.customers.controller.AnalyticsController.RealtimeMetricsResponse;
import com.example.customers.controller.AnalyticsController.SalesPerformanceResponse;
import com.example.customers.controller.AnalyticsController.StatusDistributionResponse;
import com.example.customers.controller.AnalyticsController.TrendAnalysisResponse;
import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.StatusHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Analytics Service Tests")
class AnalyticsServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private SalesRepository salesRepository;
  @Mock private StatusHistoryRepository statusHistoryRepository;

  @InjectMocks private AnalyticsService analyticsService;

  private String testSalesPhone;
  private ZonedDateTime testStartDate;
  private ZonedDateTime testEndDate;

  @BeforeEach
  void setUp() {
    testSalesPhone = "+1234567890";
    testEndDate = ZonedDateTime.now();
    testStartDate = testEndDate.minusDays(30);
  }

  @Test
  @DisplayName("Should get dashboard overview for admin")
  void shouldGetDashboardOverviewForAdmin() {
    // Given
    when(customerRepository.countTotalActiveCustomers()).thenReturn(100L);
    when(customerRepository.countNewCustomersInPeriod(any(), any())).thenReturn(20L);
    when(customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED))
        .thenReturn(15L);

    // When
    DashboardOverviewResponse response = analyticsService.getDashboardOverview(null, 30);

    // Then
    assertNotNull(response);
    assertEquals(100L, response.getTotalCustomers());
    assertEquals(20L, response.getNewCustomersThisPeriod());
    assertEquals(20L, response.getActiveCustomers()); // Simplified logic
    assertEquals(new BigDecimal("15.00"), response.getConversionRate());

    verify(customerRepository).countTotalActiveCustomers();
    verify(customerRepository, times(3)).countNewCustomersInPeriod(any(), any());
    verify(customerRepository, times(2))
        .countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED);
  }

  @Test
  @DisplayName("Should get dashboard overview for sales user")
  void shouldGetDashboardOverviewForSalesUser() {
    // Given
    when(customerRepository.countTotalActiveCustomersBySales(testSalesPhone)).thenReturn(50L);
    when(customerRepository.countNewCustomersInPeriodBySales(eq(testSalesPhone), any(), any()))
        .thenReturn(10L);
    when(customerRepository.countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
            CustomerStatus.CERTIFIED, testSalesPhone))
        .thenReturn(8L);

    // When
    DashboardOverviewResponse response = analyticsService.getDashboardOverview(testSalesPhone, 30);

    // Then
    assertNotNull(response);
    assertEquals(50L, response.getTotalCustomers());
    assertEquals(10L, response.getNewCustomersThisPeriod());
    assertEquals(10L, response.getActiveCustomers());
    assertEquals(new BigDecimal("16.00"), response.getConversionRate());

    verify(customerRepository).countTotalActiveCustomersBySales(testSalesPhone);
    verify(customerRepository, times(3))
        .countNewCustomersInPeriodBySales(eq(testSalesPhone), any(), any());
    verify(customerRepository, times(2))
        .countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
            CustomerStatus.CERTIFIED, testSalesPhone);
  }

  @Test
  @DisplayName("Should get status distribution for admin")
  void shouldGetStatusDistributionForAdmin() {
    // Given
    List<Object[]> mockResults = new ArrayList<>();
    mockResults.add(new Object[] {CustomerStatus.NEW, 30L});
    mockResults.add(new Object[] {CustomerStatus.NOTIFIED, 20L});
    mockResults.add(new Object[] {CustomerStatus.CERTIFIED, 15L});

    when(customerRepository.countCustomersByStatus()).thenReturn(mockResults);

    // When
    StatusDistributionResponse response = analyticsService.getStatusDistribution(null);

    // Then
    assertNotNull(response);
    assertEquals(65L, response.getTotalCustomers());
    assertEquals(30L, response.getStatusCounts().get("NEW"));
    assertEquals(20L, response.getStatusCounts().get("NOTIFIED"));
    assertEquals(15L, response.getStatusCounts().get("CERTIFIED"));

    verify(customerRepository).countCustomersByStatus();
  }

  @Test
  @DisplayName("Should get status distribution for sales user")
  void shouldGetStatusDistributionForSalesUser() {
    // Given
    List<Object[]> mockResults = new ArrayList<>();
    mockResults.add(new Object[] {CustomerStatus.NEW, 15L});
    mockResults.add(new Object[] {CustomerStatus.CERTIFIED, 8L});

    when(customerRepository.countCustomersByStatusForSales(testSalesPhone)).thenReturn(mockResults);

    // When
    StatusDistributionResponse response = analyticsService.getStatusDistribution(testSalesPhone);

    // Then
    assertNotNull(response);
    assertEquals(23L, response.getTotalCustomers());
    assertEquals(15L, response.getStatusCounts().get("NEW"));
    assertEquals(8L, response.getStatusCounts().get("CERTIFIED"));

    verify(customerRepository).countCustomersByStatusForSales(testSalesPhone);
  }

  @Test
  @DisplayName("Should get customer trends for admin")
  void shouldGetCustomerTrendsForAdmin() {
    // Given
    List<Object[]> mockResults = new ArrayList<>();
    mockResults.add(new Object[] {LocalDate.now().minusDays(2).toString(), 5L});
    mockResults.add(new Object[] {LocalDate.now().minusDays(1).toString(), 3L});

    when(customerRepository.getCustomerTrendsByDate(any(), any())).thenReturn(mockResults);
    when(customerRepository.countCustomersCreatedBefore(any())).thenReturn(50L);
    when(customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED))
        .thenReturn(10L);

    // When
    TrendAnalysisResponse response = analyticsService.getCustomerTrends(null, 7, "daily");

    // Then
    assertNotNull(response);
    assertEquals(2, response.getDataPoints().size());
    assertEquals("daily", response.getGranularity());
    assertEquals(7, response.getTotalDays());

    verify(customerRepository).getCustomerTrendsByDate(any(), any());
    verify(customerRepository).countCustomersCreatedBefore(any());
  }

  @Test
  @DisplayName("Should get customer trends for sales user")
  void shouldGetCustomerTrendsForSalesUser() {
    // Given
    List<Object[]> mockResults = new ArrayList<>();
    mockResults.add(new Object[] {LocalDate.now().minusDays(1).toString(), 2L});

    when(customerRepository.getCustomerTrendsByDateForSales(eq(testSalesPhone), any(), any()))
        .thenReturn(mockResults);
    when(customerRepository.countCustomersCreatedBeforeForSales(eq(testSalesPhone), any()))
        .thenReturn(25L);
    when(customerRepository.countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
            CustomerStatus.CERTIFIED, testSalesPhone))
        .thenReturn(5L);

    // When
    TrendAnalysisResponse response = analyticsService.getCustomerTrends(testSalesPhone, 7, "daily");

    // Then
    assertNotNull(response);
    assertEquals(1, response.getDataPoints().size());
    assertEquals(2L, response.getDataPoints().get(0).getNewCustomers());

    verify(customerRepository).getCustomerTrendsByDateForSales(eq(testSalesPhone), any(), any());
    verify(customerRepository).countCustomersCreatedBeforeForSales(eq(testSalesPhone), any());
  }

  @Test
  @DisplayName("Should get sales performance for admin")
  void shouldGetSalesPerformanceForAdmin() {
    // Given
    when(customerRepository.countTotalActiveCustomers()).thenReturn(100L);
    when(customerRepository.countNewCustomersInPeriod(any(), any())).thenReturn(20L);
    when(customerRepository.countConversionsInPeriod(eq(CustomerStatus.CERTIFIED), any(), any()))
        .thenReturn(15L);
    when(customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED))
        .thenReturn(15L);

    List<Object[]> statusResults = new ArrayList<>();
    statusResults.add(new Object[] {CustomerStatus.NEW, 30L});
    statusResults.add(new Object[] {CustomerStatus.CERTIFIED, 15L});
    when(customerRepository.countCustomersByStatus()).thenReturn(statusResults);

    // When
    SalesPerformanceResponse response = analyticsService.getSalesPerformance(null, 30);

    // Then
    assertNotNull(response);
    assertEquals("ADMIN", response.getSalesPhone());
    assertEquals(100L, response.getTotalCustomers());
    assertEquals(20L, response.getNewCustomers());
    assertEquals(15L, response.getConversions());
    assertEquals(new BigDecimal("15.00"), response.getConversionRate());

    verify(customerRepository).countTotalActiveCustomers();
    verify(customerRepository).countNewCustomersInPeriod(any(), any());
    verify(customerRepository).countConversionsInPeriod(eq(CustomerStatus.CERTIFIED), any(), any());
  }

  @Test
  @DisplayName("Should get sales performance for sales user")
  void shouldGetSalesPerformanceForSalesUser() {
    // Given
    when(customerRepository.countTotalActiveCustomersBySales(testSalesPhone)).thenReturn(50L);
    when(customerRepository.countNewCustomersInPeriodBySales(eq(testSalesPhone), any(), any()))
        .thenReturn(10L);
    when(customerRepository.countConversionsInPeriodBySales(
            eq(CustomerStatus.CERTIFIED), eq(testSalesPhone), any(), any()))
        .thenReturn(8L);
    when(customerRepository.countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
            CustomerStatus.CERTIFIED, testSalesPhone))
        .thenReturn(8L);

    List<Object[]> statusResults = new ArrayList<>();
    statusResults.add(new Object[] {CustomerStatus.NEW, 20L});
    statusResults.add(new Object[] {CustomerStatus.CERTIFIED, 8L});
    when(customerRepository.countCustomersByStatusForSales(testSalesPhone))
        .thenReturn(statusResults);

    // When
    SalesPerformanceResponse response = analyticsService.getSalesPerformance(testSalesPhone, 30);

    // Then
    assertNotNull(response);
    assertEquals(testSalesPhone, response.getSalesPhone());
    assertEquals(50L, response.getTotalCustomers());
    assertEquals(10L, response.getNewCustomers());
    assertEquals(8L, response.getConversions());
    assertEquals(new BigDecimal("16.00"), response.getConversionRate());

    verify(customerRepository).countTotalActiveCustomersBySales(testSalesPhone);
    verify(customerRepository).countNewCustomersInPeriodBySales(eq(testSalesPhone), any(), any());
    verify(customerRepository)
        .countConversionsInPeriodBySales(
            eq(CustomerStatus.CERTIFIED), eq(testSalesPhone), any(), any());
  }

  @Test
  @DisplayName("Should get sales leaderboard")
  void shouldGetSalesLeaderboard() {
    // Given
    List<Object[]> mockResults = new ArrayList<>();
    mockResults.add(new Object[] {"+1111111111", 50L, 10L, 20.0});
    mockResults.add(new Object[] {"+2222222222", 30L, 8L, 26.67});

    when(salesRepository.getSalesLeaderboardData(any(), eq("customers"))).thenReturn(mockResults);

    // When
    LeaderboardResponse response = analyticsService.getSalesLeaderboard(30, "customers");

    // Then
    assertNotNull(response);
    assertEquals(2, response.getRankings().size());
    assertEquals(30, response.getTotalDays());
    assertEquals("customers", response.getMetric());

    // Check first ranking
    assertEquals("+1111111111", response.getRankings().get(0).getSalesPhone());
    assertEquals(50L, response.getRankings().get(0).getTotalCustomers());
    assertEquals(10L, response.getRankings().get(0).getConversions());
    assertEquals(new BigDecimal("20.00"), response.getRankings().get(0).getConversionRate());
    assertEquals(1, response.getRankings().get(0).getRank());

    verify(salesRepository).getSalesLeaderboardData(any(), eq("customers"));
  }

  @Test
  @DisplayName("Should get real-time metrics for admin")
  void shouldGetRealtimeMetricsForAdmin() {
    // Given
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();
    when(customerRepository.countNewCustomersInPeriod(eq(todayStr), eq(todayStr))).thenReturn(5L);
    when(customerRepository.countConversionsInPeriod(eq(CustomerStatus.CERTIFIED), any(), any()))
        .thenReturn(2L);

    // When
    RealtimeMetricsResponse response = analyticsService.getRealtimeMetrics(null);

    // Then
    assertNotNull(response);
    assertEquals(5L, response.getActiveCustomersToday());
    assertEquals(5L, response.getNewCustomersToday());
    assertEquals(2L, response.getConversionsToday());
    assertNotNull(response.getLastUpdated());

    verify(customerRepository, times(2)).countNewCustomersInPeriod(eq(todayStr), eq(todayStr));
    verify(customerRepository).countConversionsInPeriod(eq(CustomerStatus.CERTIFIED), any(), any());
  }

  @Test
  @DisplayName("Should get real-time metrics for sales user")
  void shouldGetRealtimeMetricsForSalesUser() {
    // Given
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();
    when(customerRepository.countNewCustomersInPeriodBySales(
            eq(testSalesPhone), eq(todayStr), eq(todayStr)))
        .thenReturn(3L);
    when(customerRepository.countConversionsInPeriodBySales(
            eq(CustomerStatus.CERTIFIED), eq(testSalesPhone), any(), any()))
        .thenReturn(1L);

    // When
    RealtimeMetricsResponse response = analyticsService.getRealtimeMetrics(testSalesPhone);

    // Then
    assertNotNull(response);
    assertEquals(3L, response.getActiveCustomersToday());
    assertEquals(3L, response.getNewCustomersToday());
    assertEquals(1L, response.getConversionsToday());
    assertNotNull(response.getLastUpdated());

    verify(customerRepository, times(2))
        .countNewCustomersInPeriodBySales(eq(testSalesPhone), eq(todayStr), eq(todayStr));
    verify(customerRepository)
        .countConversionsInPeriodBySales(
            eq(CustomerStatus.CERTIFIED), eq(testSalesPhone), any(), any());
  }

  @Test
  @DisplayName("Should handle zero total customers in conversion rate calculation")
  void shouldHandleZeroTotalCustomersInConversionRateCalculation() {
    // Given
    when(customerRepository.countTotalActiveCustomers()).thenReturn(0L);
    when(customerRepository.countNewCustomersInPeriod(any(), any())).thenReturn(0L);

    // When
    DashboardOverviewResponse response = analyticsService.getDashboardOverview(null, 30);

    // Then
    assertNotNull(response);
    assertEquals(0L, response.getTotalCustomers());
    assertEquals(BigDecimal.ZERO, response.getConversionRate());

    verify(customerRepository).countTotalActiveCustomers();
  }

  @Test
  @DisplayName("Should handle empty status distribution results")
  void shouldHandleEmptyStatusDistributionResults() {
    // Given
    when(customerRepository.countCustomersByStatus()).thenReturn(new ArrayList<>());

    // When
    StatusDistributionResponse response = analyticsService.getStatusDistribution(null);

    // Then
    assertNotNull(response);
    assertEquals(0L, response.getTotalCustomers());
    assertTrue(response.getStatusCounts().isEmpty());

    verify(customerRepository).countCustomersByStatus();
  }

  @Test
  @DisplayName("Should handle empty trend results")
  void shouldHandleEmptyTrendResults() {
    // Given
    when(customerRepository.getCustomerTrendsByDate(any(), any())).thenReturn(new ArrayList<>());
    when(customerRepository.countCustomersCreatedBefore(any())).thenReturn(0L);

    // When
    TrendAnalysisResponse response = analyticsService.getCustomerTrends(null, 7, "daily");

    // Then
    assertNotNull(response);
    assertTrue(response.getDataPoints().isEmpty());
    assertEquals("daily", response.getGranularity());
    assertEquals(7, response.getTotalDays());

    verify(customerRepository).getCustomerTrendsByDate(any(), any());
  }

  @Test
  @DisplayName("Should handle empty leaderboard results")
  void shouldHandleEmptyLeaderboardResults() {
    // Given
    when(salesRepository.getSalesLeaderboardData(any(), any())).thenReturn(new ArrayList<>());

    // When
    LeaderboardResponse response = analyticsService.getSalesLeaderboard(30, "customers");

    // Then
    assertNotNull(response);
    assertTrue(response.getRankings().isEmpty());
    assertEquals(30, response.getTotalDays());
    assertEquals("customers", response.getMetric());

    verify(salesRepository).getSalesLeaderboardData(any(), any());
  }

  @Test
  @DisplayName("Should calculate period change correctly")
  void shouldCalculatePeriodChangeCorrectly() {
    // Given
    when(customerRepository.countTotalActiveCustomers()).thenReturn(100L);
    when(customerRepository.countNewCustomersInPeriod(any(), any())).thenReturn(20L);
    when(customerRepository.countByCurrentStatusAndDeletedAtIsNull(CustomerStatus.CERTIFIED))
        .thenReturn(15L);

    // When
    DashboardOverviewResponse response = analyticsService.getDashboardOverview(null, 30);

    // Then
    assertNotNull(response);
    assertNotNull(response.getPeriodChange());

    // Verify period change calculations
    PeriodChange periodChange = response.getPeriodChange();
    assertNotNull(periodChange.getTotalCustomersChange());
    assertNotNull(periodChange.getNewCustomersChange());
    assertNotNull(periodChange.getConversionRateChange());

    verify(customerRepository).countTotalActiveCustomers();
  }
}
