package com.example.customers.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.customers.config.TestSecurityConfig;
import com.example.customers.service.CustomerService;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = HealthController.class,
    excludeFilters =
        @Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {com.example.customers.security.JwtAuthenticationFilter.class}))
@Import(TestSecurityConfig.class)
@DisplayName("Health Controller Tests")
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DataSource dataSource;

  @MockBean private CustomerService customerService;

  @Test
  @DisplayName("Should return OK status for simple health check")
  void shouldReturnOkStatusForSimpleHealthCheck() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/health/simple"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }
}
