package com.example.customers.service.leaderboard;

import com.example.customers.controller.AnalyticsController.LeaderboardResponse;
import com.example.customers.controller.AnalyticsController.SalesPerformanceEntry;
import com.example.customers.repository.SalesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

  private final SalesRepository salesRepository;

  @Autowired
  public LeaderboardService(SalesRepository salesRepository) {
    this.salesRepository = salesRepository;
  }

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

  public LeaderboardResponse getSalesLeaderboardByYear(int year, String metric) {
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

  public LeaderboardResponse getSalesLeaderboardByMonth(int year, int month, String metric) {
    String yearStr = String.valueOf(year);
    String monthStr = String.valueOf(month);
    if (month < 10) {
      monthStr = "0" + monthStr;
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
}
