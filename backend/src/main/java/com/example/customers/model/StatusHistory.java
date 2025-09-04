package com.example.customers.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private CustomerStatus fromStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private CustomerStatus toStatus;

    private String reason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private ZonedDateTime changedAt;

    public StatusHistory() {}

    public StatusHistory(Customer customer, CustomerStatus fromStatus, CustomerStatus toStatus, String reason) {
        this.customer = customer;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public CustomerStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(CustomerStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public CustomerStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(CustomerStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ZonedDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(ZonedDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public String toString() {
        return "StatusHistory{" +
                "id=" + id +
                ", customer=" + (customer != null ? customer.getId() : null) +
                ", fromStatus=" + fromStatus +
                ", toStatus=" + toStatus +
                ", reason='" + reason + '\'' +
                ", changedAt=" + changedAt +
                '}';
    }
}