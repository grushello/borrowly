package com.borrowly.model.item;

import com.borrowly.model.BaseEntity;
import com.borrowly.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table (name = "items")
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString (exclude = {"category", "owner", "images"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Item extends BaseEntity {

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

    // Timestamps are set in @PrePersist/@PreUpdate below: Hibernate's @CreationTimestamp and
    // @UpdateTimestamp are only generated once the INSERT/UPDATE runs.
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    public void addImage(ItemImage image) {
        images.add(image);
        image.setItem(this);
    }

    public Optional<ItemImage> getPrimaryImage() {
        return images.stream().filter(ItemImage::isPrimary).findFirst();
    }
}
