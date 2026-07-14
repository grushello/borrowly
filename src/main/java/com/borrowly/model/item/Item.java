package com.borrowly.model.item;

import com.borrowly.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table (name = "items")
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString (exclude = {"category", "owner"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Item {

    @Id
    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Setter
    @NotBlank
    @Size (min = 3, max = 120)
    @Column (length = 120)
    private String title;

    @Setter
    @Size (max = 4000)
    @Column (length = 4000)
    private String description;

    @Setter
    @NotNull
    @DecimalMin(value = "0.00")
    @Column (precision = 12, scale = 2)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal pricePerDay;

    @Setter
    @NotNull
    @DecimalMin(value = "0.00")
    @Column (precision = 12, scale = 2)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal depositAmount;

    @Setter
    @NotNull
    @DecimalMin(value = "0.00")
    @Column (precision = 12, scale = 2)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal finePerDay;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    private ItemCondition condition;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ItemStatus status = ItemStatus.ACTIVE;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
