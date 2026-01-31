package com.example.customers.repository;

import com.example.customers.entity.MonthlyAgentPerformance;
import com.example.customers.entity.MonthlyAgentPerformanceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for MonthlyAgentPerformance entity.
 *
 * <p>Provides methods for managing monthly performance metrics by customer agent.
 */
@Repository
public interface MonthlyAgentPerformanceRepository
    extends JpaRepository<MonthlyAgentPerformance, MonthlyAgentPerformanceId> {

  /**
   * Find all monthly agent performance records ordered by month ascending and customer agent
   * ascending.
   *
   * @return List of all monthly agent performance records ordered by month and agent
   */
  List<MonthlyAgentPerformance> findAllByOrderByMonthAscCustomerAgentAsc();

  /**
   * Find all records for a specific customer agent ordered by month ascending.
   *
   * @param customerAgent The customer agent identifier
   * @return List of performance records for the agent ordered by month
   */
  List<MonthlyAgentPerformance> findByCustomerAgentOrderByMonthAsc(String customerAgent);

  /**
   * Find all records for a specific month ordered by customer agent ascending.
   *
   * @param month The month in 'yyyy-MM' format
   * @return List of performance records for the month ordered by agent
   */
  List<MonthlyAgentPerformance> findByMonthOrderByCustomerAgentAsc(String month);

  /**
   * Find all records within a month range ordered by month ascending and customer agent ascending.
   *
   * @param startMonth The start month in 'yyyy-MM' format (inclusive)
   * @param endMonth The end month in 'yyyy-MM' format (inclusive)
   * @return List of performance records within the range ordered by month and agent
   */
  List<MonthlyAgentPerformance> findByMonthBetweenOrderByMonthAscCustomerAgentAsc(
      String startMonth, String endMonth);

  /**
   * Find all unique customer agents.
   *
   * @return List of unique customer agent identifiers
   */
  @Query("SELECT DISTINCT m.customerAgent FROM MonthlyAgentPerformance m ORDER BY m.customerAgent")
  List<String> findAllDistinctCustomerAgents();

  /**
   * Get the month range available in the table.
   *
   * @return Object array with [minMonth, maxMonth]
   */
  @Query(
      value =
          """
          SELECT MIN(month) as minMonth, MAX(month) as maxMonth
          FROM monthly_agent_performance
          """,
      nativeQuery = true)
  Object[] findMonthRange();
}
