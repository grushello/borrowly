package com.borrowly.model.rental;

import com.borrowly.model.item.Item;
import com.borrowly.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
        name = "rentals",
        indexes = {
                @Index(name = "idx_rental_borrower", columnList = "borrower_id"),
                @Index(name = "idx_rental_item", columnList = "item_id"),
                @Index(name = "idx_rental_status", columnList = "status"),
                @Index(name = "idx_rental_dates", columnList = "start_date, end_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"item", "borrower"})
@EqualsAndHashCode(of = "id")
public class Rental {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "Item is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @NotNull(message = "Borrower is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @NotBlank(message = "Item title is required")
    @Size(max = 120, message = "Item title cannot exceed 120 characters")
    @Column(name = "item_title", nullable = false, length = 120)
    private String itemTitle;

    @NotNull(message = "Daily price is required")
    @DecimalMin(value = "0.00", message = "Daily price cannot be negative")
    @Column(name = "daily_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyPrice;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.00", message = "Deposit amount cannot be negative")
    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @NotNull(message = "Fine per day is required")
    @DecimalMin(value = "0.00", message = "Fine per day cannot be negative")
    @Column(name = "fine_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal finePerDay;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.00", message = "Total price cannot be negative")
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RentalStatus.ACTIVE;
        }
    }

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}