package com.borrowly.model.rental;

import com.borrowly.model.item.Item;
import com.borrowly.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rentals")
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(exclude = {"item", "borrower"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Rental {

    @Id
    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate actualReturnDate;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String itemTitle;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyPrice;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finePerDay;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RentalStatus status = RentalStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = RentalStatus.ACTIVE;
        }
    }

    public void returnItem(LocalDate date) {
        this.actualReturnDate = date;
        this.status = RentalStatus.RETURNED;
    }

    public void markOverdue() {
        if (status != RentalStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE rental can become overdue, was " + status);
        }
        this.status = RentalStatus.OVERDUE;
    }

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Actual return date must be on or after start date")
    public boolean isActualReturnDateValid() {
        if (actualReturnDate == null || startDate == null) {
            return true;
        }
        return !actualReturnDate.isBefore(startDate);
    }
}