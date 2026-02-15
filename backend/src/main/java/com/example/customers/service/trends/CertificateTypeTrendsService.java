package com.example.customers.service.trends;

import com.example.customers.controller.AnalyticsController.CertificateTypeTrendsResponse;
import com.example.customers.controller.AnalyticsController.TrendDataPoint;
import com.example.customers.entity.MonthlyCertifiedCountByCertificateType;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.MonthlyCertifiedCountByCertificateTypeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CertificateTypeTrendsService {

  private final CustomerRepository customerRepository;
  private final MonthlyCertifiedCountByCertificateTypeRepository
      monthlyCertifiedCountByCertificateTypeRepository;

  @Autowired
  public CertificateTypeTrendsService(
      CustomerRepository customerRepository,
      MonthlyCertifiedCountByCertificateTypeRepository
          monthlyCertifiedCountByCertificateTypeRepository) {
    this.customerRepository = customerRepository;
    this.monthlyCertifiedCountByCertificateTypeRepository =
        monthlyCertifiedCountByCertificateTypeRepository;
  }

  public CertificateTypeTrendsResponse getCustomerTrendsByCertificateType(int days) {
    return getCertificateTypeTrendsFromNewTable();
  }

  private CertificateTypeTrendsResponse getCertificateTypeTrendsFromNewTable() {
    List<MonthlyCertifiedCountByCertificateType> monthlyCounts =
        monthlyCertifiedCountByCertificateTypeRepository
            .findAllByOrderByMonthAscCertificateTypeAsc();

    // OPTIMIZATION: Get unsettled count ONCE outside all loops (cached)
    // This avoids N×M queries where N=certificate types, M=months
    // Example: 10 types × 12 months = 120 queries → now just 1 query (cached)
    long totalUnsettledCustomers = getUnsettledCustomers(0);

    Map<String, List<TrendDataPoint>> trendsByType = new HashMap<>();

    Map<String, List<MonthlyCertifiedCountByCertificateType>> groupedByType = new HashMap<>();
    for (MonthlyCertifiedCountByCertificateType count : monthlyCounts) {
      String certType = count.getCertificateType();
      groupedByType.computeIfAbsent(certType, k -> new ArrayList<>()).add(count);
    }

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

        String[] parts = monthStr.split("-");
        LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

        // Use the pre-fetched unsettled count instead of querying in nested loop
        BigDecimal conversionRate = calculateConversionRate(runningTotal, totalUnsettledCustomers);

        dataPoints.add(
            new TrendDataPoint(date, newCertifications, runningTotal, conversionRate, 0, 0));
      }

      trendsByType.put(certificateType, dataPoints);
    }

    return new CertificateTypeTrendsResponse(trendsByType, 0);
  }

  /**
   * Get count of unsettled customers with caching. Shared cache across all analytics services to
   * avoid duplicate queries.
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
