package com.borrowly.model.rental;

import com.borrowly.model.BaseEntity;
import com.borrowly.model.item.Item;
import com.borrowly.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rental_requests")
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(exclude = {"item", "borrower"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RentalRequest extends BaseEntity {

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RentalRequestStatus status = RentalRequestStatus.PENDING;

    // Timestamps are set in @PrePersist/@PreUpdate below: Hibernate's @CreationTimestamp and
    // @UpdateTimestamp are only generated once the INSERT/UPDATE runs.
    private LocalDateTime requestedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = RentalRequestStatus.PENDING;
        }
        LocalDateTime now = LocalDateTime.now();
        this.requestedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        requirePending("approved");
        this.status = RentalRequestStatus.APPROVED;
    }

    public void reject() {
        requirePending("rejected");
        this.status = RentalRequestStatus.REJECTED;
    }

    public void cancel() {
        requirePending("canceled");
        this.status = RentalRequestStatus.CANCELED;
    }

    public boolean isPending() {
        return status == RentalRequestStatus.PENDING;
    }

    private void requirePending(String action) {
        if (status != RentalRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending requests can be " + action + ", was " + status);
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
