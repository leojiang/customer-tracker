package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.customers.controller.AnalyticsController.TrendAnalysisResponse;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyAgentPerformanceRepository;
import com.example.customers.repository.MonthlyCertifiedCountByCertificateTypeRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.StatusHistoryRepository;
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
  @Mock private MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

  @Mock
  private MonthlyCertifiedCountByCertificateTypeRepository
      monthlyCertifiedCountByCertificateTypeRepository;

  @Mock private MonthlyAgentPerformanceRepository monthlyAgentPerformanceRepository;

  @InjectMocks private AnalyticsService analyticsService;

  @Test
  @DisplayName("testMethod with monthly granularity should return TrendAnalysisResponse")
  void testMethod_WithMonthlyGranularity_ShouldReturnTrendAnalysisResponse() {
    // Arrange
    String granularity = "monthly";
    int days = 30;

    // Act
    TrendAnalysisResponse result = analyticsService.testMethod(days, granularity);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getDataPoints(), "Data points should not be null");
    assertEquals("monthly", result.getGranularity(), "Granularity should be monthly");
  }

  @Test
  @DisplayName(
      "testMethod with MONTHLY (uppercase) granularity should return TrendAnalysisResponse")
  void testMethod_WithUpperCaseMonthlyGranularity_ShouldReturnTrendAnalysisResponse() {
    // Arrange
    String granularity = "MONTHLY";
    int days = 30;

    // Act
    TrendAnalysisResponse result = analyticsService.testMethod(days, granularity);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getDataPoints(), "Data points should not be null");
    assertEquals("monthly", result.getGranularity(), "Granularity should be monthly");
  }

  @Test
  @DisplayName("testMethod with MixedCase monthly granularity should return TrendAnalysisResponse")
  void testMethod_WithMixedCaseMonthlyGranularity_ShouldReturnTrendAnalysisResponse() {
    // Arrange
    String granularity = "MoNtHlY";
    int days = 30;

    // Act
    TrendAnalysisResponse result = analyticsService.testMethod(days, granularity);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertEquals("monthly", result.getGranularity(), "Granularity should be monthly");
  }

  @Test
  @DisplayName("testMethod with daily granularity should return TrendAnalysisResponse")
  void testMethod_WithDailyGranularity_ShouldReturnTrendAnalysisResponse() {
    // Arrange
    String granularity = "daily";
    int days = 7;

    // Act
    TrendAnalysisResponse result = analyticsService.testMethod(days, granularity);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getDataPoints(), "Data points should not be null");
    assertEquals(days, result.getTotalDays(), "Total days should match input");
  }

  @Test
  @DisplayName("testMethod with weekly granularity should return TrendAnalysisResponse")
  void testMethod_WithWeeklyGranularity_ShouldReturnTrendAnalysisResponse() {
    // Arrange
    String granularity = "weekly";
    int days = 14;

    // Act
    TrendAnalysisResponse result = analyticsService.testMethod(days, granularity);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getDataPoints(), "Data points should not be null");
  }
}
