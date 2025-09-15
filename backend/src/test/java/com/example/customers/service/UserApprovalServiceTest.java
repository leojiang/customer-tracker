package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.customers.model.ApprovalAction;
import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.model.UserApprovalHistory;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.UserApprovalHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Approval Service Tests")
class UserApprovalServiceTest {

  @Mock private SalesRepository salesRepository;
  @Mock private UserApprovalHistoryRepository historyRepository;

  @InjectMocks private UserApprovalService userApprovalService;

  private Sales testUser;
  private String testUserPhone;
  private String testAdminPhone;
  private String testReason;

  @BeforeEach
  void setUp() {
    testUserPhone = "+1234567890";
    testAdminPhone = "+9999999999";
    testReason = "Test approval reason";

    testUser = new Sales();
    testUser.setId(UUID.randomUUID());
    testUser.setPhone(testUserPhone);
    testUser.setPassword("hashedPassword");
    testUser.setRole(SalesRole.SALES);
    testUser.setApprovalStatus(ApprovalStatus.PENDING);
    testUser.setCreatedAt(ZonedDateTime.now());
  }

  @Test
  @DisplayName("Should get pending approvals successfully")
  void shouldGetPendingApprovalsSuccessfully() {
    // Given
    List<Sales> pendingUsers = Arrays.asList(testUser);
    Page<Sales> expectedPage = new PageImpl<>(pendingUsers);
    when(salesRepository.findByApprovalStatusOrderByCreatedAtDesc(
            eq(ApprovalStatus.PENDING), any(Pageable.class)))
        .thenReturn(expectedPage);

    // When
    Page<Sales> result = userApprovalService.getPendingApprovals(Pageable.unpaged());

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testUser, result.getContent().get(0));

    verify(salesRepository)
        .findByApprovalStatusOrderByCreatedAtDesc(eq(ApprovalStatus.PENDING), any(Pageable.class));
  }

  @Test
  @DisplayName("Should get users by approval status successfully")
  void shouldGetUsersByApprovalStatusSuccessfully() {
    // Given
    List<Sales> approvedUsers = Arrays.asList(testUser);
    Page<Sales> expectedPage = new PageImpl<>(approvedUsers);
    when(salesRepository.findByApprovalStatusOrderByCreatedAtDesc(
            eq(ApprovalStatus.APPROVED), any(Pageable.class)))
        .thenReturn(expectedPage);

    // When
    Page<Sales> result =
        userApprovalService.getUsersByApprovalStatus(ApprovalStatus.APPROVED, Pageable.unpaged());

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testUser, result.getContent().get(0));

    verify(salesRepository)
        .findByApprovalStatusOrderByCreatedAtDesc(eq(ApprovalStatus.APPROVED), any(Pageable.class));
  }

  @Test
  @DisplayName("Should approve user successfully")
  void shouldApproveUserSuccessfully() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    Sales result = userApprovalService.approveUser(testUserPhone, testAdminPhone, testReason);

    // Then
    assertNotNull(result);
    assertEquals(ApprovalStatus.APPROVED, result.getApprovalStatus());
    assertEquals(testAdminPhone, result.getApprovedByPhone());
    assertNotNull(result.getApprovedAt());
    assertNotNull(result.getStatusUpdatedAt());
    assertNull(result.getRejectionReason());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository).save(testUser);
    verify(historyRepository)
        .save(
            argThat(
                history ->
                    history.getUserPhone().equals(testUserPhone)
                        && history.getAction() == ApprovalAction.APPROVED
                        && history.getAdminPhone().equals(testAdminPhone)
                        && history.getReason().equals(testReason)));
  }

  @Test
  @DisplayName("Should throw exception when approving non-existent user")
  void shouldThrowExceptionWhenApprovingNonExistentUser() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> userApprovalService.approveUser(testUserPhone, testAdminPhone, testReason));

    assertEquals("User not found: " + testUserPhone, exception.getMessage());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository, never()).save(any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when approving already approved user")
  void shouldThrowExceptionWhenApprovingAlreadyApprovedUser() {
    // Given
    testUser.setApprovalStatus(ApprovalStatus.APPROVED);
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> userApprovalService.approveUser(testUserPhone, testAdminPhone, testReason));

    assertEquals("User is already approved", exception.getMessage());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository, never()).save(any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should reject user successfully")
  void shouldRejectUserSuccessfully() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    Sales result = userApprovalService.rejectUser(testUserPhone, testAdminPhone, testReason);

    // Then
    assertNotNull(result);
    assertEquals(ApprovalStatus.REJECTED, result.getApprovalStatus());
    assertEquals(testAdminPhone, result.getApprovedByPhone());
    assertNull(result.getApprovedAt());
    assertEquals(testReason, result.getRejectionReason());
    assertNotNull(result.getStatusUpdatedAt());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository).save(testUser);
    verify(historyRepository)
        .save(
            argThat(
                history ->
                    history.getUserPhone().equals(testUserPhone)
                        && history.getAction() == ApprovalAction.REJECTED
                        && history.getAdminPhone().equals(testAdminPhone)
                        && history.getReason().equals(testReason)));
  }

  @Test
  @DisplayName("Should throw exception when rejecting non-existent user")
  void shouldThrowExceptionWhenRejectingNonExistentUser() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> userApprovalService.rejectUser(testUserPhone, testAdminPhone, testReason));

    assertEquals("User not found: " + testUserPhone, exception.getMessage());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository, never()).save(any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should reset user status successfully")
  void shouldResetUserStatusSuccessfully() {
    // Given
    testUser.setApprovalStatus(ApprovalStatus.REJECTED);
    testUser.setRejectionReason("Previous rejection");
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    Sales result = userApprovalService.resetUserStatus(testUserPhone, testAdminPhone, testReason);

    // Then
    assertNotNull(result);
    assertEquals(ApprovalStatus.PENDING, result.getApprovalStatus());
    assertNull(result.getApprovedByPhone());
    assertNull(result.getApprovedAt());
    assertNull(result.getRejectionReason());
    assertNotNull(result.getStatusUpdatedAt());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository).save(testUser);
    verify(historyRepository)
        .save(
            argThat(
                history ->
                    history.getUserPhone().equals(testUserPhone)
                        && history.getAction() == ApprovalAction.RESET
                        && history.getAdminPhone().equals(testAdminPhone)
                        && history.getReason().equals(testReason)));
  }

  @Test
  @DisplayName("Should throw exception when resetting non-existent user")
  void shouldThrowExceptionWhenResettingNonExistentUser() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> userApprovalService.resetUserStatus(testUserPhone, testAdminPhone, testReason));

    assertEquals("User not found: " + testUserPhone, exception.getMessage());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository, never()).save(any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should bulk approve users successfully")
  void shouldBulkApproveUsersSuccessfully() {
    // Given
    List<String> userPhones = Arrays.asList(testUserPhone, "+1111111111");

    // Create a second user for the second phone
    Sales secondUser = new Sales();
    secondUser.setId(UUID.randomUUID());
    secondUser.setPhone("+1111111111");
    secondUser.setPassword("hashedPassword");
    secondUser.setRole(SalesRole.SALES);
    secondUser.setApprovalStatus(ApprovalStatus.PENDING);
    secondUser.setCreatedAt(ZonedDateTime.now());

    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.findByPhone("+1111111111")).thenReturn(Optional.of(secondUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    int approvedCount = userApprovalService.bulkApprove(userPhones, testAdminPhone, testReason);

    // Then
    assertEquals(2, approvedCount);

    verify(salesRepository, times(2)).findByPhone(any());
    verify(salesRepository, times(2)).save(any());
    verify(historyRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("Should handle partial bulk approval failure")
  void shouldHandlePartialBulkApprovalFailure() {
    // Given
    List<String> userPhones = Arrays.asList(testUserPhone, "+1111111111");
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.findByPhone("+1111111111")).thenReturn(Optional.empty());
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    int approvedCount = userApprovalService.bulkApprove(userPhones, testAdminPhone, testReason);

    // Then
    assertEquals(1, approvedCount);

    verify(salesRepository, times(2)).findByPhone(any());
    verify(salesRepository, times(1)).save(any());
    verify(historyRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("Should bulk reject users successfully")
  void shouldBulkRejectUsersSuccessfully() {
    // Given
    List<String> userPhones = Arrays.asList(testUserPhone, "+1111111111");
    when(salesRepository.findByPhone(any())).thenReturn(Optional.of(testUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    int rejectedCount = userApprovalService.bulkReject(userPhones, testAdminPhone, testReason);

    // Then
    assertEquals(2, rejectedCount);

    verify(salesRepository, times(2)).findByPhone(any());
    verify(salesRepository, times(2)).save(any());
    verify(historyRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("Should get user approval history successfully")
  void shouldGetUserApprovalHistorySuccessfully() {
    // Given
    List<UserApprovalHistory> history = Arrays.asList(new UserApprovalHistory());
    when(historyRepository.findByUserPhoneOrderByActionTimestampDesc(testUserPhone))
        .thenReturn(history);

    // When
    List<UserApprovalHistory> result = userApprovalService.getUserApprovalHistory(testUserPhone);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    verify(historyRepository).findByUserPhoneOrderByActionTimestampDesc(testUserPhone);
  }

  @Test
  @DisplayName("Should get approval statistics successfully")
  void shouldGetApprovalStatisticsSuccessfully() {
    // Given
    when(salesRepository.countByApprovalStatus(ApprovalStatus.PENDING)).thenReturn(5L);
    when(salesRepository.countByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(20L);
    when(salesRepository.countByApprovalStatus(ApprovalStatus.REJECTED)).thenReturn(3L);
    when(historyRepository.findRecentActions(any(ZonedDateTime.class)))
        .thenReturn(Arrays.asList(new UserApprovalHistory(), new UserApprovalHistory()));

    // When
    UserApprovalService.ApprovalStatistics result = userApprovalService.getApprovalStatistics();

    // Then
    assertNotNull(result);
    assertEquals(5L, result.getPendingCount());
    assertEquals(20L, result.getApprovedCount());
    assertEquals(3L, result.getRejectedCount());
    assertEquals(2, result.getRecentActivityCount());
    assertEquals(86.96, result.getApprovalRate(), 0.01); // 20/(20+3)*100

    verify(salesRepository).countByApprovalStatus(ApprovalStatus.PENDING);
    verify(salesRepository).countByApprovalStatus(ApprovalStatus.APPROVED);
    verify(salesRepository).countByApprovalStatus(ApprovalStatus.REJECTED);
    verify(historyRepository).findRecentActions(any(ZonedDateTime.class));
  }

  @Test
  @DisplayName("Should handle zero approval rate in statistics")
  void shouldHandleZeroApprovalRateInStatistics() {
    // Given
    when(salesRepository.countByApprovalStatus(ApprovalStatus.PENDING)).thenReturn(5L);
    when(salesRepository.countByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(0L);
    when(salesRepository.countByApprovalStatus(ApprovalStatus.REJECTED)).thenReturn(0L);
    when(historyRepository.findRecentActions(any(ZonedDateTime.class))).thenReturn(Arrays.asList());

    // When
    UserApprovalService.ApprovalStatistics result = userApprovalService.getApprovalStatistics();

    // Then
    assertNotNull(result);
    assertEquals(5L, result.getPendingCount());
    assertEquals(0L, result.getApprovedCount());
    assertEquals(0L, result.getRejectedCount());
    assertEquals(0, result.getRecentActivityCount());
    assertEquals(0.0, result.getApprovalRate());

    verify(salesRepository).countByApprovalStatus(ApprovalStatus.PENDING);
    verify(salesRepository).countByApprovalStatus(ApprovalStatus.APPROVED);
    verify(salesRepository).countByApprovalStatus(ApprovalStatus.REJECTED);
    verify(historyRepository).findRecentActions(any(ZonedDateTime.class));
  }

  @Test
  @DisplayName("Should get recent activity successfully")
  void shouldGetRecentActivitySuccessfully() {
    // Given
    List<UserApprovalHistory> recentActivity = Arrays.asList(new UserApprovalHistory());
    when(historyRepository.findRecentActions(any(ZonedDateTime.class))).thenReturn(recentActivity);

    // When
    List<UserApprovalHistory> result = userApprovalService.getRecentActivity(7);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    verify(historyRepository).findRecentActions(any(ZonedDateTime.class));
  }

  @Test
  @DisplayName("Should handle empty bulk operations")
  void shouldHandleEmptyBulkOperations() {
    // Given
    List<String> emptyList = Arrays.asList();

    // When
    int approvedCount = userApprovalService.bulkApprove(emptyList, testAdminPhone, testReason);
    int rejectedCount = userApprovalService.bulkReject(emptyList, testAdminPhone, testReason);

    // Then
    assertEquals(0, approvedCount);
    assertEquals(0, rejectedCount);

    verify(salesRepository, never()).findByPhone(any());
    verify(salesRepository, never()).save(any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should handle null reason in approval operations")
  void shouldHandleNullReasonInApprovalOperations() {
    // Given
    when(salesRepository.findByPhone(testUserPhone)).thenReturn(Optional.of(testUser));
    when(salesRepository.save(any(Sales.class))).thenReturn(testUser);
    when(historyRepository.save(any(UserApprovalHistory.class)))
        .thenReturn(new UserApprovalHistory());

    // When
    Sales result = userApprovalService.approveUser(testUserPhone, testAdminPhone, null);

    // Then
    assertNotNull(result);
    assertEquals(ApprovalStatus.APPROVED, result.getApprovalStatus());

    verify(salesRepository).findByPhone(testUserPhone);
    verify(salesRepository).save(testUser);
    verify(historyRepository)
        .save(
            argThat(
                history ->
                    history.getUserPhone().equals(testUserPhone)
                        && history.getAction() == ApprovalAction.APPROVED
                        && history.getAdminPhone().equals(testAdminPhone)
                        && history.getReason() == null));
  }
}
