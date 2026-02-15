package com.example.customers.service.trends;

import com.example.customers.controller.AnalyticsController.TrendAnalysisResponse;
import com.example.customers.controller.AnalyticsController.TrendDataPoint;
import com.example.customers.entity.MonthlyCertifiedCount;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrendAnalysisService {

  private final CustomerRepository customerRepository;
  private final MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

  @Autowired
  public TrendAnalysisService(
      CustomerRepository customerRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository) {
    this.customerRepository = customerRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
  }

  public TrendAnalysisResponse getCustomerTrends(int days, String granularity) {
    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);

    if (isMonthly) {
      return getMonthlyTrendsFromNewTable();
    }

    return getTrendsFromCustomerTable(days, granularity);
  }

  private TrendAnalysisResponse getMonthlyTrendsFromNewTable() {
    List<MonthlyCertifiedCount> monthlyCounts =
        monthlyCertifiedCountRepository.findAllByOrderByMonthAsc();

    // OPTIMIZATION: Get unsettled count once outside the loop (cached)
    // This avoids N+1 queries - was calling getUnsettledCustomers() inside loop
    long totalUnsettledCustomers = getUnsettledCustomers(0);

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal = 0;

    for (MonthlyCertifiedCount monthlyCount : monthlyCounts) {
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

      String[] parts = monthStr.split("-");
      LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

      // Use the pre-fetched unsettled count instead of querying in loop
      BigDecimal conversionRateAtDate =
          calculateConversionRate(runningTotal, totalUnsettledCustomers);

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

  private TrendAnalysisResponse getTrendsFromCustomerTable(int days, String granularity) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();

    boolean isMonthly = "monthly".equalsIgnoreCase(granularity);
    List<Object[]> results;

    if (isMonthly) {
      results = customerRepository.getCustomerTrendsByMonth(startDateStr, endDateStr);
    } else {
      results = customerRepository.getCustomerTrendsByDate(startDateStr, endDateStr);
    }

    // OPTIMIZATION: Get unsettled count once outside the loop (cached)
    long totalUnsettledCustomers = getUnsettledCustomers(0);

    List<TrendDataPoint> dataPoints = new ArrayList<>();
    long runningTotal = customerRepository.countCustomersCreatedBefore(startDate);

    for (Object[] row : results) {
      String periodStr = (String) row[0];
      Long newCustomers = ((Number) row[1]).longValue();
      runningTotal += newCustomers;

      LocalDate date;
      if (isMonthly) {
        String[] parts = periodStr.split("-");
        date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
      } else {
        date = LocalDate.parse(periodStr);
      }

      // Use the pre-fetched unsettled count instead of querying in loop
      BigDecimal conversionRateAtDate =
          calculateConversionRate(runningTotal, totalUnsettledCustomers);

      dataPoints.add(
          new TrendDataPoint(date, newCustomers, runningTotal, conversionRateAtDate, 0, 0));
    }

    return new TrendAnalysisResponse(dataPoints, granularity, days);
  }

  /**
   * Get count of unsettled customers with caching. This is an expensive query so we cache the
   * result for 5 minutes.
   */
  @Cacheable(value = "unsettledCustomerCount", key = "'total'")
  private long getUnsettledCustomers(long totalCustomers) {
    return customerRepository.countNotCertifiedCustomers();
  }

  private BigDecimal calculateConversionRate(long totalCustomers, long unsettledCustomers) {
    if (totalCustomers == 0) {
      return BigDecimal.ZERO;
    }

    long settledCustomers = totalCustomers - unsettledCustomers;

    return BigDecimal.valueOf(settledCustomers)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
  }
}
