package com.example.customers.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.customers.config.TestSecurityConfig;
import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.EducationLevel;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.model.StatusHistory;
import com.example.customers.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = CustomerController.class,
    excludeFilters =
        @Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {com.example.customers.security.JwtAuthenticationFilter.class}))
@Import(TestSecurityConfig.class)
@DisplayName("Customer Controller Tests")
class CustomerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CustomerService customerService;

  @Autowired private ObjectMapper objectMapper;

  private Customer testCustomer;
  private UUID testCustomerId;

  @BeforeEach
  void setUp() {
    // Set up mock authenticated user
    Sales mockSales = new Sales();
    mockSales.setId(UUID.randomUUID());
    mockSales.setPhone("+9999999999");
    mockSales.setRole(SalesRole.ADMIN);

    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(mockSales, null, "ADMIN");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    testCustomerId = UUID.randomUUID();
    testCustomer = new Customer();
    testCustomer.setId(testCustomerId);
    testCustomer.setName("John Doe");
    testCustomer.setPhone("+1234567890");
    testCustomer.setCompany("Test Company");
    testCustomer.setBusinessRequirements("Need CRM solution");
    testCustomer.setBusinessType("Technology");
    testCustomer.setAge(30);
    testCustomer.setEducation(EducationLevel.BACHELOR);
    testCustomer.setGender("Male");
    testCustomer.setLocation("New York");
    testCustomer.setCurrentStatus(CustomerStatus.CUSTOMER_CALLED);
    testCustomer.setSalesPhone("+9999999999"); // Match the mock authenticated user's phone
    testCustomer.setCreatedAt(ZonedDateTime.now());
    testCustomer.setUpdatedAt(ZonedDateTime.now());
  }

  @Test
  @DisplayName("Should get customers with pagination successfully")
  void shouldGetCustomersWithPaginationSuccessfully() throws Exception {
    // Given
    List<Customer> customers = Arrays.asList(testCustomer);
    Page<Customer> customerPage = new PageImpl<>(customers, Pageable.ofSize(5), 1);

    when(customerService.searchCustomers(
            any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
        .thenReturn(customerPage);

    // When & Then
    mockMvc
        .perform(get("/api/customers").param("page", "1").param("limit", "5"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.limit").value(5))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.items[0].name").value("John Doe"))
        .andExpect(jsonPath("$.items[0].phone").value("+1234567890"));

    verify(customerService)
        .searchCustomers(
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            any(),
            eq(false),
            eq(null),
            eq(null),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should get customers with search parameters")
  void shouldGetCustomersWithSearchParameters() throws Exception {
    // Given
    List<Customer> customers = Arrays.asList(testCustomer);
    Page<Customer> customerPage = new PageImpl<>(customers, Pageable.ofSize(10), 1);

    when(customerService.searchCustomers(
            eq("john"),
            eq("123"),
            eq(CustomerStatus.CUSTOMER_CALLED),
            eq("test"),
            any(),
            eq(false),
            eq(null),
            eq(null),
            any(Pageable.class)))
        .thenReturn(customerPage);

    // When & Then
    mockMvc
        .perform(
            get("/api/customers")
                .param("q", "john")
                .param("phone", "123")
                .param("status", "CUSTOMER_CALLED")
                .param("company", "test")
                .param("includeDeleted", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));

    verify(customerService)
        .searchCustomers(
            eq("john"),
            eq("123"),
            eq(CustomerStatus.CUSTOMER_CALLED),
            eq("test"),
            any(),
            eq(false),
            eq(null),
            eq(null),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should get single customer by ID successfully")
  void shouldGetSingleCustomerByIdSuccessfully() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));

    // When & Then
    mockMvc
        .perform(get("/api/customers/{id}", testCustomerId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(testCustomerId.toString()))
        .andExpect(jsonPath("$.name").value("John Doe"))
        .andExpect(jsonPath("$.phone").value("+1234567890"))
        .andExpect(jsonPath("$.currentStatus").value("CUSTOMER_CALLED"));

    verify(customerService).getCustomerById(testCustomerId);
  }

  @Test
  @DisplayName("Should return 404 when customer not found by ID")
  void shouldReturn404WhenCustomerNotFoundById() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/customers/{id}", testCustomerId)).andExpect(status().isNotFound());

    verify(customerService).getCustomerById(testCustomerId);
  }

  @Test
  @DisplayName("Should create customer successfully")
  void shouldCreateCustomerSuccessfully() throws Exception {
    // Given
    CustomerController.CreateCustomerRequest request =
        new CustomerController.CreateCustomerRequest();
    request.setName("Jane Smith");
    request.setPhone("+9876543210");
    request.setCompany("New Company");
    request.setBusinessRequirements("Need inventory system");
    request.setCurrentStatus(CustomerStatus.CUSTOMER_CALLED);

    when(customerService.createCustomer(any(Customer.class), anyString())).thenReturn(testCustomer);

    // When & Then
    mockMvc
        .perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("John Doe")); // Returns the mock testCustomer

    verify(customerService).createCustomer(any(Customer.class), anyString());
  }

  @Test
  @DisplayName("Should return 400 when creating customer with duplicate phone")
  void shouldReturn400WhenCreatingCustomerWithDuplicatePhone() throws Exception {
    // Given
    CustomerController.CreateCustomerRequest request =
        new CustomerController.CreateCustomerRequest();
    request.setName("Jane Smith");
    request.setPhone("+1234567890");

    when(customerService.createCustomer(any(Customer.class), anyString()))
        .thenThrow(new IllegalArgumentException("Customer with phone +1234567890 already exists"));

    // When & Then
    mockMvc
        .perform(
            post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(customerService).createCustomer(any(Customer.class), anyString());
  }

  @Test
  @DisplayName("Should update customer successfully")
  void shouldUpdateCustomerSuccessfully() throws Exception {
    // Given
    CustomerController.UpdateCustomerRequest request =
        new CustomerController.UpdateCustomerRequest();
    request.setName("Updated Name");
    request.setPhone("+1111111111");
    request.setCompany("Updated Company");

    Customer updatedCustomer = new Customer();
    updatedCustomer.setId(testCustomerId);
    updatedCustomer.setName("Updated Name");
    updatedCustomer.setPhone("+1111111111");

    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.updateCustomer(eq(testCustomerId), any(Customer.class)))
        .thenReturn(updatedCustomer);

    // When & Then
    mockMvc
        .perform(
            patch("/api/customers/{id}", testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.phone").value("+1111111111"));

    verify(customerService).updateCustomer(eq(testCustomerId), any(Customer.class));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent customer")
  void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
    // Given
    CustomerController.UpdateCustomerRequest request =
        new CustomerController.UpdateCustomerRequest();
    request.setName("Updated Name");
    request.setPhone("+1234567890"); // Add valid phone number to pass validation

    when(customerService.getCustomerById(testCustomerId))
        .thenReturn(Optional.empty()); // This will trigger the 404
    when(customerService.updateCustomer(eq(testCustomerId), any(Customer.class)))
        .thenThrow(new EntityNotFoundException("Customer not found"));

    // When & Then
    mockMvc
        .perform(
            patch("/api/customers/{id}", testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    verify(customerService).getCustomerById(testCustomerId);
    verify(customerService, never()).updateCustomer(any(), any());
  }

  @Test
  @DisplayName("Should delete customer successfully")
  void shouldDeleteCustomerSuccessfully() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    doNothing().when(customerService).deleteCustomer(testCustomerId);

    // When & Then
    mockMvc
        .perform(delete("/api/customers/{id}", testCustomerId))
        .andExpect(status().isNoContent());

    verify(customerService).deleteCustomer(testCustomerId);
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent customer")
  void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId))
        .thenReturn(Optional.empty()); // This will trigger the 404
    doThrow(new EntityNotFoundException("Customer not found"))
        .when(customerService)
        .deleteCustomer(testCustomerId);

    // When & Then
    mockMvc.perform(delete("/api/customers/{id}", testCustomerId)).andExpect(status().isNotFound());

    verify(customerService).getCustomerById(testCustomerId);
    verify(customerService, never()).deleteCustomer(any());
  }

  @Test
  @DisplayName("Should restore customer successfully")
  void shouldRestoreCustomerSuccessfully() throws Exception {
    // Given
    when(customerService.getCustomerByIdIncludingDeleted(testCustomerId))
        .thenReturn(Optional.of(testCustomer));
    when(customerService.restoreCustomer(testCustomerId)).thenReturn(testCustomer);

    // When & Then
    mockMvc
        .perform(post("/api/customers/{id}/restore", testCustomerId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("John Doe"));

    verify(customerService).restoreCustomer(testCustomerId);
  }

  @Test
  @DisplayName("Should transition status successfully")
  void shouldTransitionStatusSuccessfully() throws Exception {
    // Given
    CustomerController.StatusTransitionRequest request =
        new CustomerController.StatusTransitionRequest();
    request.setToStatus(CustomerStatus.REPLIED_TO_CUSTOMER);
    request.setReason("Customer responded positively");

    Customer updatedCustomer = new Customer();
    updatedCustomer.setId(testCustomerId);
    updatedCustomer.setCurrentStatus(CustomerStatus.REPLIED_TO_CUSTOMER);

    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.transitionStatus(
            testCustomerId, CustomerStatus.REPLIED_TO_CUSTOMER, "Customer responded positively"))
        .thenReturn(updatedCustomer);

    // When & Then
    mockMvc
        .perform(
            post("/api/customers/{id}/status-transition", testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.currentStatus").value("REPLIED_TO_CUSTOMER"));

    verify(customerService)
        .transitionStatus(
            testCustomerId, CustomerStatus.REPLIED_TO_CUSTOMER, "Customer responded positively");
  }

  @Test
  @DisplayName("Should return 400 for invalid status transition")
  void shouldReturn400ForInvalidStatusTransition() throws Exception {
    // Given
    CustomerController.StatusTransitionRequest request =
        new CustomerController.StatusTransitionRequest();
    request.setToStatus(CustomerStatus.BUSINESS_DONE);
    request.setReason("Invalid transition");

    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.transitionStatus(
            testCustomerId, CustomerStatus.BUSINESS_DONE, "Invalid transition"))
        .thenThrow(
            new IllegalArgumentException(
                "Invalid transition from Customer called to Business done"));

    // When & Then
    mockMvc
        .perform(
            post("/api/customers/{id}/status-transition", testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(customerService)
        .transitionStatus(testCustomerId, CustomerStatus.BUSINESS_DONE, "Invalid transition");
  }

  @Test
  @DisplayName("Should get status history successfully")
  void shouldGetStatusHistorySuccessfully() throws Exception {
    // Given
    List<StatusHistory> statusHistory =
        Arrays.asList(
            createStatusHistory(testCustomer, null, CustomerStatus.CUSTOMER_CALLED, "Initial"),
            createStatusHistory(
                testCustomer,
                CustomerStatus.CUSTOMER_CALLED,
                CustomerStatus.REPLIED_TO_CUSTOMER,
                "Responded"));

    Page<StatusHistory> statusHistoryPage = new PageImpl<>(statusHistory);
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.getCustomerStatusHistory(eq(testCustomerId), any(Pageable.class)))
        .thenReturn(statusHistoryPage);

    // When & Then
    mockMvc
        .perform(get("/api/customers/{id}/status-history", testCustomerId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].toStatus").value("CUSTOMER_CALLED"))
        .andExpect(jsonPath("$[1].fromStatus").value("CUSTOMER_CALLED"))
        .andExpect(jsonPath("$[1].toStatus").value("REPLIED_TO_CUSTOMER"));

    verify(customerService).getCustomerStatusHistory(eq(testCustomerId), any(Pageable.class));
  }

  @Test
  @DisplayName("Should get valid transitions successfully")
  void shouldGetValidTransitionsSuccessfully() throws Exception {
    // Given
    Set<CustomerStatus> validTransitions =
        Set.of(CustomerStatus.REPLIED_TO_CUSTOMER, CustomerStatus.LOST);

    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.getValidTransitions(testCustomerId)).thenReturn(validTransitions);

    // When & Then
    mockMvc
        .perform(get("/api/customers/{id}/valid-transitions", testCustomerId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));

    verify(customerService).getValidTransitions(testCustomerId);
  }

  @Test
  @DisplayName("Should return 404 when getting valid transitions for non-existent customer")
  void shouldReturn404WhenGettingValidTransitionsForNonExistentCustomer() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId))
        .thenReturn(Optional.empty()); // This will trigger the 404
    when(customerService.getValidTransitions(testCustomerId))
        .thenThrow(new EntityNotFoundException("Customer not found"));

    // When & Then
    mockMvc
        .perform(get("/api/customers/{id}/valid-transitions", testCustomerId))
        .andExpect(status().isNotFound());

    verify(customerService).getCustomerById(testCustomerId);
    verify(customerService, never()).getValidTransitions(any());
  }

  @Test
  @DisplayName("Should validate transition successfully")
  void shouldValidateTransitionSuccessfully() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.isValidTransition(testCustomerId, CustomerStatus.REPLIED_TO_CUSTOMER))
        .thenReturn(true);

    // When & Then
    mockMvc
        .perform(
            get(
                "/api/customers/{id}/can-transition-to/{status}",
                testCustomerId,
                "REPLIED_TO_CUSTOMER"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.valid").value(true));

    verify(customerService).isValidTransition(testCustomerId, CustomerStatus.REPLIED_TO_CUSTOMER);
  }

  @Test
  @DisplayName("Should return invalid transition validation")
  void shouldReturnInvalidTransitionValidation() throws Exception {
    // Given
    when(customerService.getCustomerById(testCustomerId)).thenReturn(Optional.of(testCustomer));
    when(customerService.isValidTransition(testCustomerId, CustomerStatus.BUSINESS_DONE))
        .thenReturn(false);

    // When & Then
    mockMvc
        .perform(
            get("/api/customers/{id}/can-transition-to/{status}", testCustomerId, "BUSINESS_DONE"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.valid").value(false));

    verify(customerService).isValidTransition(testCustomerId, CustomerStatus.BUSINESS_DONE);
  }

  @Test
  @DisplayName("Should get statistics successfully")
  void shouldGetStatisticsSuccessfully() throws Exception {
    // Given
    CustomerService.CustomerStatistics stats = new CustomerService.CustomerStatistics();
    stats.setTotalCustomers(10);
    stats.setRecentlyUpdatedCount(5);
    stats.addStatusCount(CustomerStatus.CUSTOMER_CALLED, 3L);
    stats.addStatusCount(CustomerStatus.REPLIED_TO_CUSTOMER, 2L);

    when(customerService.getCustomerStatistics(false)).thenReturn(stats);

    // When & Then
    mockMvc
        .perform(get("/api/customers/statistics"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalCustomers").value(10))
        .andExpect(jsonPath("$.recentlyUpdatedCount").value(5))
        .andExpect(jsonPath("$.statusCounts.CUSTOMER_CALLED").value(3))
        .andExpect(jsonPath("$.statusCounts.REPLIED_TO_CUSTOMER").value(2));

    verify(customerService).getCustomerStatistics(false);
  }

  @Test
  @DisplayName("Should get recent customers successfully")
  void shouldGetRecentCustomersSuccessfully() throws Exception {
    // Given
    List<Customer> customers = Arrays.asList(testCustomer);
    Page<Customer> customerPage = new PageImpl<>(customers, Pageable.ofSize(20), 1);

    when(customerService.getRecentlyUpdatedCustomers(eq(7), any(Pageable.class)))
        .thenReturn(customerPage);

    // When & Then
    mockMvc
        .perform(get("/api/customers/recent").param("days", "7"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.total").value(1));

    verify(customerService).getRecentlyUpdatedCustomers(eq(7), any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle pagination parameter validation")
  void shouldHandlePaginationParameterValidation() throws Exception {
    // Given
    List<Customer> customers = Arrays.asList(testCustomer);
    Page<Customer> customerPage = new PageImpl<>(customers, Pageable.ofSize(20), 1);

    when(customerService.searchCustomers(
            any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
        .thenReturn(customerPage);

    // When & Then - Test with invalid page (should default to 1)
    mockMvc
        .perform(get("/api/customers").param("page", "-1").param("limit", "200")) // Above max limit
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.limit").value(100)); // Should be capped at 100

    // Verify pagination was corrected
    verify(customerService)
        .searchCustomers(
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(false),
            eq(null),
            eq(null),
            argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 100));
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
