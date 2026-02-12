package com.example.customers.service.dashboard;

import com.example.customers.controller.AnalyticsController.DashboardOverviewResponse;
import com.example.customers.controller.AnalyticsController.PeriodChange;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardOverviewService {

  private final CustomerRepository customerRepository;
  private final MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;

  @Autowired
  public DashboardOverviewService(
      CustomerRepository customerRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository) {
    this.customerRepository = customerRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
  }

  public DashboardOverviewResponse getDashboardOverview(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);
    ZonedDateTime previousStartDate = startDate.minusDays(days);

    String startDateStr = startDate.toLocalDate().toString();
    String endDateStr = endDate.toLocalDate().toString();
    String previousStartDateStr = previousStartDate.toLocalDate().toString();
    String startDateComparisonStr = startDate.toLocalDate().toString();

    List<com.example.customers.entity.MonthlyCertifiedCount> monthlyCounts =
        monthlyCertifiedCountRepository.findAllByOrderByMonthAsc();
    long totalCustomers =
        monthlyCounts.stream().mapToLong(mc -> mc.getCertifiedCount().longValue()).sum();

    long newCustomersThisPeriod =
        customerRepository.countNewCustomersInPeriod(startDateStr, endDateStr);

    long unsettledCustomers = getUnsettledCustomers(totalCustomers);
    BigDecimal conversionRate = calculateConversionRate(totalCustomers, unsettledCustomers);

    long newCustomersPreviousPeriod =
        customerRepository.countNewCustomersInPeriod(previousStartDateStr, startDateComparisonStr);

    long previousTotalCustomers = totalCustomers - newCustomersThisPeriod;
    long previousUnsettledCustomers = getUnsettledCustomers(previousTotalCustomers);
    BigDecimal previousConversionRate =
        calculateConversionRate(previousTotalCustomers, previousUnsettledCustomers);

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

  private BigDecimal calculateConversionRate(long totalCustomers, long unsettledCustomers) {
    if (totalCustomers == 0) {
      return BigDecimal.ZERO;
    }

    long settledCustomers = totalCustomers - unsettledCustomers;

    return BigDecimal.valueOf(settledCustomers)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP);
  }

  private long getUnsettledCustomers(long totalCustomers) {
    return customerRepository.countNotCertifiedCustomers();
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
