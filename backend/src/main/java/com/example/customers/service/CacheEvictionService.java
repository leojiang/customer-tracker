package com.example.customers.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for managing cache eviction and refresh. Periodically clears analytics caches to ensure
 * data freshness.
 */
@Service
public class CacheEvictionService {

  /**
   * Evict unsettled customer count cache every 5 minutes. This ensures the cache stays fresh while
   * still providing performance benefits.
   */
  @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
  @CacheEvict(value = "unsettledCustomerCount", allEntries = true)
  public void evictUnsettledCustomerCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict status distribution cache every 10 minutes. */
  @Scheduled(fixedRate = 10 * 60 * 1000) // Every 10 minutes
  @CacheEvict(value = "statusDistribution", allEntries = true)
  public void evictStatusDistributionCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict dashboard metrics cache every 5 minutes. */
  @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
  @CacheEvict(value = "dashboardMetrics", allEntries = true)
  public void evictDashboardMetricsCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict trend analysis cache every 5 minutes. */
  @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
  @CacheEvict(value = "trendAnalysis", allEntries = true)
  public void evictTrendAnalysisCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict certificate type trends cache every 5 minutes. */
  @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
  @CacheEvict(value = "certificateTypeTrends", allEntries = true)
  public void evictCertificateTypeTrendsCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict leaderboard cache every 10 minutes. */
  @Scheduled(fixedRate = 10 * 60 * 1000) // Every 10 minutes
  @CacheEvict(value = "leaderboard", allEntries = true)
  public void evictLeaderboardCache() {
    // Cache evicted - will be refreshed on next access
  }

  /** Evict agent performance cache every 5 minutes. */
  @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
  @CacheEvict(value = "agentPerformance", allEntries = true)
  public void evictAgentPerformanceCache() {
    // Cache evicted - will be refreshed on next access
  }
}
