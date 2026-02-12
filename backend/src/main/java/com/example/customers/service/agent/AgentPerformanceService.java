package com.example.customers.service.agent;

import com.example.customers.controller.AnalyticsController.AgentPerformanceTrendsResponse;
import com.example.customers.controller.AnalyticsController.TrendDataPoint;
import com.example.customers.entity.MonthlyAgentPerformance;
import com.example.customers.repository.MonthlyAgentPerformanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AgentPerformanceService {

  private final MonthlyAgentPerformanceRepository monthlyAgentPerformanceRepository;

  @Autowired
  public AgentPerformanceService(
      MonthlyAgentPerformanceRepository monthlyAgentPerformanceRepository) {
    this.monthlyAgentPerformanceRepository = monthlyAgentPerformanceRepository;
  }

  public AgentPerformanceTrendsResponse getAgentPerformanceTrends() {
    List<MonthlyAgentPerformance> monthlyData =
        monthlyAgentPerformanceRepository.findAllByOrderByMonthAscCustomerAgentAsc();

    Map<String, List<TrendDataPoint>> trendsByAgent = new HashMap<>();

    for (MonthlyAgentPerformance record : monthlyData) {
      String agent = record.getCustomerAgent();
      String monthStr = record.getMonth();

      String[] parts = monthStr.split("-");
      LocalDate date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);

      Long newCustomers = record.getNewCustomers().longValue();
      Long conversions = record.getConversions().longValue();
      BigDecimal conversionRate = record.getConversionRate();
      Long totalCustomers = record.getTotalCustomers().longValue();

      TrendDataPoint dataPoint =
          new TrendDataPoint(date, newCustomers, totalCustomers, conversionRate, 0, 0);

      trendsByAgent.computeIfAbsent(agent, k -> new ArrayList<>()).add(dataPoint);
    }

    List<String> agents = new ArrayList<>(trendsByAgent.keySet());
    java.util.Collections.sort(agents);

    return new AgentPerformanceTrendsResponse(trendsByAgent, agents);
  }
}
