package com.example.customers.dto.customer;

import com.example.customers.model.Customer;
import java.util.List;

public class CustomerPageResponse {

  private List<Customer> items;

  private long total;

  private int page;

  private int limit;

  private int totalPages;

  public CustomerPageResponse(
      List<Customer> items, long total, int page, int limit, int totalPages) {
    this.items = items;
    this.total = total;
    this.page = page;
    this.limit = limit;
    this.totalPages = totalPages;
  }

  public List<Customer> getItems() {
    return items;
  }

  public void setItems(List<Customer> items) {
    this.items = items;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }
}
