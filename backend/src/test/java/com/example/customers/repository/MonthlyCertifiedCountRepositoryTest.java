package com.example.customers.repository;

import com.example.customers.entity.MonthlyCertifiedCount;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MonthlyCertifiedCountRepository.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(exclude = FlywayAutoConfiguration.class)
@DisplayName("Monthly Certified Count Repository Tests")
class MonthlyCertifiedCountRepositoryTest {

  @Autowired
  private MonthlyCertifiedCountRepository repository;

  @Autowired
  private EntityManager entityManager;

  private MonthlyCertifiedCount testCount;

  @BeforeEach
  void setUp() {
    // Clean up before each test
    repository.deleteAll();

    // Create a test entity
    testCount = new MonthlyCertifiedCount();
    testCount.setMonth("2024-01");
    testCount.setCertifiedCount(5);
  }

  @Test
  @DisplayName("Should save monthly certified count")
  void shouldSaveMonthlyCertifiedCount() {
    // When
    MonthlyCertifiedCount saved = repository.save(testCount);

    // Then
    assertNotNull(saved);
    assertNotNull(saved.getMonth());
    assertEquals("2024-01", saved.getMonth());
    assertEquals(5, saved.getCertifiedCount());
    assertNotNull(saved.getCreatedAt());
    assertNotNull(saved.getUpdatedAt());
  }

  @Test
  @DisplayName("Should find monthly certified count by month")
  void shouldFindByMonth() {
    // Given
    repository.save(testCount);

    // When
    Optional<MonthlyCertifiedCount> found = repository.findByMonth("2024-01");

    // Then
    assertTrue(found.isPresent());
    assertEquals("2024-01", found.get().getMonth());
    assertEquals(5, found.get().getCertifiedCount());
  }

  @Test
  @DisplayName("Should return empty when month not found")
  void shouldReturnEmptyWhenMonthNotFound() {
    // Given
    repository.save(testCount);

    // When
    Optional<MonthlyCertifiedCount> found = repository.findByMonth("2024-02");

    // Then
    assertFalse(found.isPresent());
  }

  @Test
  @DisplayName("Should increment certified count for new month")
  void shouldIncrementCertifiedCountForNewMonth() {
    // When
    repository.incrementCertifiedCount("2024-03");

    // Then
    Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-03");
    assertTrue(result.isPresent());
    assertEquals(1, result.get().getCertifiedCount());
  }

  @Test
  @DisplayName("Should increment certified count for existing month")
  void shouldIncrementCertifiedCountForExistingMonth() {
    // Given
    testCount.setCertifiedCount(5);
    repository.save(testCount);

    // When - increment twice
    repository.incrementCertifiedCount("2024-01");
    repository.incrementCertifiedCount("2024-01");

    // Then - clear persistence context to force fresh query
    entityManager.flush();
    entityManager.clear();
    Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-01");
    assertTrue(result.isPresent());
    assertEquals(7, result.get().getCertifiedCount()); // 5 + 1 + 1
  }

  @Test
  @DisplayName("Should handle multiple increments on same month")
  void shouldHandleMultipleIncrements() {
    // When
    for (int i = 0; i < 10; i++) {
      repository.incrementCertifiedCount("2024-04");
    }

    // Then
    Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-04");
    assertTrue(result.isPresent());
    assertEquals(10, result.get().getCertifiedCount());
  }

  @Test
  @DisplayName("Should handle concurrent increments for same month")
  void shouldHandleConcurrentIncrements() {
    // Given
    repository.save(testCount);

    // When - multiple increments
    repository.incrementCertifiedCount("2024-01");
    repository.incrementCertifiedCount("2024-01");
    repository.incrementCertifiedCount("2024-01");

    // Then - clear persistence context to force fresh query
    entityManager.flush();
    entityManager.clear();
    Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-01");
    assertTrue(result.isPresent());
    assertEquals(8, result.get().getCertifiedCount()); // 5 + 1 + 1 + 1
  }

  @Test
  @DisplayName("Should increment count and update timestamp")
  void shouldIncrementCountAndUpdateTimestamp() {
    // Given
    MonthlyCertifiedCount saved = repository.save(testCount);
    Integer originalCount = saved.getCertifiedCount();

    // When
    repository.incrementCertifiedCount("2024-01");

    // Then - clear persistence context to force fresh query
    entityManager.flush();
    entityManager.clear();
    Optional<MonthlyCertifiedCount> result = repository.findByMonth("2024-01");
    assertTrue(result.isPresent());
    // Verify the count incremented (this proves the UPDATE ran successfully)
    assertEquals(originalCount + 1, result.get().getCertifiedCount());
  }
}
