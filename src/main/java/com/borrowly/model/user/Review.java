package com.borrowly.model.user;

import com.borrowly.model.BaseEntity;
import com.borrowly.model.rental.Rental;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(exclude = {"reviewer", "rental"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Review extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Size(max = 2000)
    @Column(length = 2000)
    private String comment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // @PrePersist, not @CreationTimestamp: the latter is only generated once the INSERT runs.
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
