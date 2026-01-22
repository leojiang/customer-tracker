package com.example.customers.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.StatusHistory;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.StatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.*;
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
@DisplayName("Customer Service Tests")
class CustomerServiceTest {

  @Mock private CustomerRepository customerRepository;

  @Mock private StatusHistoryRepository statusHistoryRepository;

  @Mock private StatusTransitionValidator transitionValidator;

  @InjectMocks private CustomerService customerService;

  private Customer testCustomer;
  private UUID testCustomerId;

  @BeforeEach
  void setUp() {
    testCustomerId = UUID.randomUUID();
    testCustomer = new Customer();
    testCustomer.setId(testCustomerId);
    testCustomer.setName("John Doe");
    testCustomer.setPhone("+1234567890");
    testCustomer.setCertificateIssuer("Test Certificate Issuer");
    testCustomer.setCurrentStatus(CustomerStatus.NEW);
    testCustomer.setCreatedAt(ZonedDateTime.now());
    testCustomer.setUpdatedAt(ZonedDateTime.now());
  }

  @Test
  @DisplayName("Should create customer successfully")
  void shouldCreateCustomerSuccessfully() {
    // Given
    Customer newCustomer = new Customer();
    newCustomer.setName("Jane Smith");
    newCustomer.setPhone("+9876543210");

    when(customerRepository.findByPhoneIncludingDeleted("+9876543210"))
        .thenReturn(Optional.empty());
    when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

    // When
    Customer result = customerService.createCustomer(newCustomer);

    // Then
    assertNotNull(result);
    assertEquals(CustomerStatus.NEW, newCustomer.getCurrentStatus());
    verify(customerRepository).findByPhoneIncludingDeleted("+9876543210");
    verify(customerRepository).save(newCustomer);
    verify(statusHistoryRepository).save(any(StatusHistory.class));
  }

  @Test
  @DisplayName("Should throw exception when creating customer with duplicate phone")
  void shouldThrowExceptionWhenCreatingCustomerWithDuplicatePhone() {
    // Given
    Customer newCustomer = new Customer();
    newCustomer.setPhone("+1234567890");

    when(customerRepository.findByPhoneIncludingDeleted("+1234567890"))
        .thenReturn(Optional.of(testCustomer));

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> customerService.createCustomer(newCustomer));

    assertTrue(exception.getMessage().contains("already exists"));
    verify(customerRepository, never()).save(any());
    verify(statusHistoryRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get customer by ID successfully")
  void shouldGetCustomerByIdSuccessfully() {
    // Given
    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));

    // When
    Optional<Customer> result = customerService.getCustomerById(testCustomerId);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testCustomer, result.get());
    verify(customerRepository).findById(testCustomerId);
  }

  @Test
  @DisplayName("Should return empty when customer not found by ID")
  void shouldReturnEmptyWhenCustomerNotFoundById() {
    // Given
    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.empty());

    // When
    Optional<Customer> result = customerService.getCustomerById(testCustomerId);

    // Then
    assertFalse(result.isPresent());
    verify(customerRepository).findById(testCustomerId);
  }

  @Test
  @DisplayName("Should update customer successfully")
  void shouldUpdateCustomerSuccessfully() {
    // Given
    Customer updateData = new Customer();
    updateData.setName("Updated Name");
    updateData.setPhone("+1111111111");
    updateData.setCertificateIssuer("Updated Certificate Issuer");

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerRepository.findByPhoneIncludingDeleted("+1111111111"))
        .thenReturn(Optional.empty());
    when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

    // When
    Customer result = customerService.updateCustomer(testCustomerId, updateData);

    // Then
    assertNotNull(result);
    verify(customerRepository).findById(testCustomerId);
    verify(customerRepository).findByPhoneIncludingDeleted("+1111111111");
    verify(customerRepository).save(testCustomer);
    assertEquals("Updated Name", testCustomer.getName());
    assertEquals("+1111111111", testCustomer.getPhone());
    assertEquals("Updated Certificate Issuer", testCustomer.getCertificateIssuer());
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent customer")
  void shouldThrowExceptionWhenUpdatingNonExistentCustomer() {
    // Given
    Customer updateData = new Customer();
    updateData.setName("Updated Name");

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> customerService.updateCustomer(testCustomerId, updateData));

    assertTrue(exception.getMessage().contains("not found"));
    verify(customerRepository).findById(testCustomerId);
    verify(customerRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when updating customer with duplicate phone")
  void shouldThrowExceptionWhenUpdatingCustomerWithDuplicatePhone() {
    // Given
    Customer otherCustomer = new Customer();
    otherCustomer.setId(UUID.randomUUID());
    otherCustomer.setPhone("+1111111111");

    Customer updateData = new Customer();
    updateData.setPhone("+1111111111");

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerRepository.findByPhoneIncludingDeleted("+1111111111"))
        .thenReturn(Optional.of(otherCustomer));

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> customerService.updateCustomer(testCustomerId, updateData));

    assertTrue(exception.getMessage().contains("already exists"));
    verify(customerRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should transition status successfully with valid transition")
  void shouldTransitionStatusSuccessfullyWithValidTransition() {
    // Given
    CustomerStatus toStatus = CustomerStatus.NOTIFIED;
    String reason = "Customer responded positively";

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(transitionValidator.isValidTransition(CustomerStatus.NEW, toStatus)).thenReturn(true);
    when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

    // When
    Customer result = customerService.transitionStatus(testCustomerId, toStatus, reason);

    // Then
    assertNotNull(result);
    assertEquals(toStatus, testCustomer.getCurrentStatus());
    verify(customerRepository).findById(testCustomerId);
    verify(transitionValidator).isValidTransition(CustomerStatus.NEW, toStatus);
    verify(customerRepository).save(testCustomer);
    verify(statusHistoryRepository).save(any(StatusHistory.class));
  }

  @Test
  @DisplayName("Should throw exception for invalid status transition")
  void shouldThrowExceptionForInvalidStatusTransition() {
    // Given
    CustomerStatus toStatus = CustomerStatus.CERTIFIED;
    String reason = "Invalid transition";
    String errorMessage = "Invalid transition from Customer called to Business done";

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(transitionValidator.isValidTransition(CustomerStatus.NEW, toStatus)).thenReturn(false);
    when(transitionValidator.getTransitionErrorMessage(CustomerStatus.NEW, toStatus))
        .thenReturn(errorMessage);

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> customerService.transitionStatus(testCustomerId, toStatus, reason));

    assertEquals(errorMessage, exception.getMessage());
    verify(customerRepository).findById(testCustomerId);
    verify(transitionValidator).isValidTransition(CustomerStatus.NEW, toStatus);
    verify(customerRepository, never()).save(any());
    verify(statusHistoryRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when transitioning status of non-existent customer")
  void shouldThrowExceptionWhenTransitioningStatusOfNonExistentCustomer() {
    // Given
    CustomerStatus toStatus = CustomerStatus.NOTIFIED;
    String reason = "Test reason";

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception =
        assertThrows(
            EntityNotFoundException.class,
            () -> customerService.transitionStatus(testCustomerId, toStatus, reason));

    assertTrue(exception.getMessage().contains("not found"));
    verify(customerRepository).findById(testCustomerId);
    verify(transitionValidator, never()).isValidTransition(any(), any());
  }

  @Test
  @DisplayName("Should get status history successfully")
  void shouldGetStatusHistorySuccessfully() {
    // Given
    List<StatusHistory> statusHistoryList =
        Arrays.asList(
            createStatusHistory(testCustomer, null, CustomerStatus.NEW, "Initial"),
            createStatusHistory(
                testCustomer, CustomerStatus.NEW, CustomerStatus.NOTIFIED, "Responded"));

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(statusHistoryRepository.findByCustomerOrderByChangedAtDesc(testCustomer))
        .thenReturn(statusHistoryList);

    // When
    List<StatusHistory> result = customerService.getCustomerStatusHistory(testCustomerId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(customerRepository).findById(testCustomerId);
    verify(statusHistoryRepository).findByCustomerOrderByChangedAtDesc(testCustomer);
  }

  @Test
  @DisplayName("Should get paginated status history successfully")
  void shouldGetPaginatedStatusHistorySuccessfully() {
    // Given
    List<StatusHistory> statusHistoryList =
        Arrays.asList(createStatusHistory(testCustomer, null, CustomerStatus.NEW, "Initial"));
    Page<StatusHistory> statusHistoryPage = new PageImpl<>(statusHistoryList);
    Pageable pageable = Pageable.ofSize(10);

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(statusHistoryRepository.findByCustomerOrderByChangedAtDesc(testCustomer, pageable))
        .thenReturn(statusHistoryPage);

    // When
    Page<StatusHistory> result = customerService.getCustomerStatusHistory(testCustomerId, pageable);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(customerRepository).findById(testCustomerId);
    verify(statusHistoryRepository).findByCustomerOrderByChangedAtDesc(testCustomer, pageable);
  }

  @Test
  @DisplayName("Should soft delete customer successfully")
  void shouldSoftDeleteCustomerSuccessfully() {
    // Given
    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

    // When
    customerService.deleteCustomer(testCustomerId);

    // Then
    assertNotNull(testCustomer.getDeletedAt());
    verify(customerRepository).findById(testCustomerId);
    verify(customerRepository).save(testCustomer);
    verify(statusHistoryRepository).save(any(StatusHistory.class));
  }

  @Test
  @DisplayName("Should restore soft-deleted customer successfully")
  void shouldRestoreSoftDeletedCustomerSuccessfully() {
    // Given
    testCustomer.softDelete(); // Mark as deleted
    when(customerRepository.findByIdIncludingDeleted(testCustomerId))
        .thenReturn(Optional.of(testCustomer));
    when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

    // When
    Customer result = customerService.restoreCustomer(testCustomerId);

    // Then
    assertNotNull(result);
    assertNull(testCustomer.getDeletedAt());
    verify(customerRepository).findByIdIncludingDeleted(testCustomerId);
    verify(customerRepository).save(testCustomer);
    verify(statusHistoryRepository).save(any(StatusHistory.class));
  }

  @Test
  @DisplayName("Should throw exception when trying to restore non-deleted customer")
  void shouldThrowExceptionWhenTryingToRestoreNonDeletedCustomer() {
    // Given
    when(customerRepository.findByIdIncludingDeleted(testCustomerId))
        .thenReturn(Optional.of(testCustomer));

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> customerService.restoreCustomer(testCustomerId));

    assertTrue(exception.getMessage().contains("not deleted"));
    verify(customerRepository).findByIdIncludingDeleted(testCustomerId);
    verify(customerRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get valid transitions successfully")
  void shouldGetValidTransitionsSuccessfully() {
    // Given
    Set<CustomerStatus> expectedTransitions =
        Set.of(CustomerStatus.NOTIFIED, CustomerStatus.ABORTED);

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(transitionValidator.getValidTransitions(CustomerStatus.NEW))
        .thenReturn(expectedTransitions);

    // When
    Set<CustomerStatus> result = customerService.getValidTransitions(testCustomerId);

    // Then
    assertNotNull(result);
    assertEquals(expectedTransitions, result);
    verify(customerRepository).findById(testCustomerId);
    verify(transitionValidator).getValidTransitions(CustomerStatus.NEW);
  }

  @Test
  @DisplayName("Should validate transition successfully")
  void shouldValidateTransitionSuccessfully() {
    // Given
    CustomerStatus toStatus = CustomerStatus.NOTIFIED;

    when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(transitionValidator.isValidTransition(CustomerStatus.NEW, toStatus)).thenReturn(true);

    // When
    boolean result = customerService.isValidTransition(testCustomerId, toStatus);

    // Then
    assertTrue(result);
    verify(customerRepository).findById(testCustomerId);
    verify(transitionValidator).isValidTransition(CustomerStatus.NEW, toStatus);
  }

  @Test
  @DisplayName("Should check phone availability correctly")
  void shouldCheckPhoneAvailabilityCorrectly() {
    // Given
    String availablePhone = "+9999999999";
    String unavailablePhone = "+1234567890";

    when(customerRepository.findByPhoneIncludingDeleted(availablePhone))
        .thenReturn(Optional.empty());
    when(customerRepository.findByPhoneIncludingDeleted(unavailablePhone))
        .thenReturn(Optional.of(testCustomer));

    // When & Then
    assertTrue(customerService.isPhoneAvailable(availablePhone));
    assertFalse(customerService.isPhoneAvailable(unavailablePhone));
  }

  @Test
  @DisplayName("Should check phone availability for customer correctly")
  void shouldCheckPhoneAvailabilityForCustomerCorrectly() {
    // Given
    String ownPhone = "+1234567890";
    String otherPhone = "+9999999999";
    Customer otherCustomer = new Customer();
    otherCustomer.setId(UUID.randomUUID());

    when(customerRepository.findByPhoneIncludingDeleted(ownPhone))
        .thenReturn(Optional.of(testCustomer));
    when(customerRepository.findByPhoneIncludingDeleted(otherPhone))
        .thenReturn(Optional.of(otherCustomer));

    // When & Then
    assertTrue(customerService.isPhoneAvailableForCustomer(ownPhone, testCustomerId));
    assertFalse(customerService.isPhoneAvailableForCustomer(otherPhone, testCustomerId));
  }

  @Test
  @DisplayName("Should get recently updated customers successfully")
  void shouldGetRecentlyUpdatedCustomersSuccessfully() {
    // Given
    List<Customer> customerList = Arrays.asList(testCustomer);
    Page<Customer> customerPage = new PageImpl<>(customerList);
    Pageable pageable = Pageable.ofSize(10);

    when(customerRepository.findRecentlyUpdated(any(ZonedDateTime.class), eq(pageable)))
        .thenReturn(customerPage);

    // When
    Page<Customer> result = customerService.getRecentlyUpdatedCustomers(7, pageable);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    verify(customerRepository).findRecentlyUpdated(any(ZonedDateTime.class), eq(pageable));
  }

  private StatusHistory createStatusHistory(
      Customer customer, CustomerStatus fromStatus, CustomerStatus toStatus, String reason) {
    StatusHistory history = new StatusHistory();
    history.setId(UUID.randomUUID());
    history.setCustomer(customer);
    history.setFromStatus(fromStatus);
    history.setToStatus(toStatus);
    history.setReason(reason);
    history.setChangedAt(ZonedDateTime.now());
    return history;
  }
}
