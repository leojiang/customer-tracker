package com.example.customers.service.dashboard;

import com.example.customers.controller.AnalyticsController.RealtimeMetricsResponse;
import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.CustomerRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RealtimeMetricsService {

  private final CustomerRepository customerRepository;

  @Autowired
  public RealtimeMetricsService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public RealtimeMetricsResponse getRealtimeMetrics() {
    LocalDate today = LocalDate.now();
    String todayStr = today.toString();

    long unsettledCustomersToday = customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    long newCustomersToday = customerRepository.countNewCustomersInPeriod(todayStr, todayStr);

    ZonedDateTime startOfDay = today.atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    long conversionsToday =
        customerRepository.countConversionsInPeriod(CustomerStatus.CERTIFIED, startOfDay, now);

    String lastUpdated = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

    return new RealtimeMetricsResponse(
        unsettledCustomersToday, newCustomersToday, conversionsToday, lastUpdated);
  }
}
