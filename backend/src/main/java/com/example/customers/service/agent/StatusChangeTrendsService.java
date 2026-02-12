package com.example.customers.service.agent;

import com.example.customers.model.CustomerStatus;
import com.example.customers.repository.StatusHistoryRepository;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatusChangeTrendsService {

  private final StatusHistoryRepository statusHistoryRepository;

  @Autowired
  public StatusChangeTrendsService(StatusHistoryRepository statusHistoryRepository) {
    this.statusHistoryRepository = statusHistoryRepository;
  }

  public Map<String, Object> getDailyStatusChangeTrends(int days) {
    ZonedDateTime endDate = ZonedDateTime.now();
    ZonedDateTime startDate = endDate.minusDays(days);

    List<Object[]> results =
        statusHistoryRepository.findDailyStatusChangeTrendsGroupedByUser(startDate, endDate);

    Map<String, Map<String, Integer>> dataByDate = new LinkedHashMap<>();
    Set<String> uniqueUsers = new LinkedHashSet<>();

    for (Object[] row : results) {
      java.sql.Date statusDate = (java.sql.Date) row[0];
      String changedBy = (String) row[1];
      CustomerStatus status = (CustomerStatus) row[2];
      Long count = ((Number) row[3]).longValue();

      uniqueUsers.add(changedBy != null ? changedBy : "System");

      String dateStr = statusDate.toString();
      String userKey = changedBy != null ? changedBy : "System";

      Map<String, Integer> dataPoint = dataByDate.computeIfAbsent(dateStr, k -> new HashMap<>());

      String key = userKey + "_" + status.name();
      dataPoint.merge(key, count.intValue(), Integer::sum);
    }

    List<Map<String, Object>> dataPoints = new ArrayList<>();

    for (int i = days - 1; i >= 0; i--) {
      ZonedDateTime currentDate = endDate.minusDays(i);
      String dateStr = currentDate.toLocalDate().toString();

      Map<String, Object> dataPoint = new HashMap<>();
      dataPoint.put("date", dateStr);

      Map<String, Integer> dayData = dataByDate.get(dateStr);
      if (dayData != null) {
        dataPoint.putAll(dayData);
      }

      dataPoints.add(dataPoint);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("dataPoints", dataPoints);
    response.put("users", new ArrayList<>(uniqueUsers));
    response.put("totalDays", days);

    return response;
  }
}
