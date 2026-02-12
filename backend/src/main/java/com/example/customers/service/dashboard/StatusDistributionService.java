package com.example.customers.service.dashboard;

import com.example.customers.controller.AnalyticsController.StatusDistributionResponse;
import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.CustomerRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatusDistributionService {

  private final CustomerRepository customerRepository;

  @Autowired
  public StatusDistributionService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public StatusDistributionResponse getStatusDistribution() {
    List<Object[]> results = customerRepository.countCustomersByStatus();

    Map<String, Long> statusCounts = new HashMap<>();
    long totalCustomers = 0;

    for (Object[] row : results) {
      CustomerStatus status = (CustomerStatus) row[0];
      Long count = ((Number) row[1]).longValue();
      statusCounts.put(status.name(), count);
      totalCustomers += count;
    }

    return new StatusDistributionResponse(statusCounts, totalCustomers);
  }
}
