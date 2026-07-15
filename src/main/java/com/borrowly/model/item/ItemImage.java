package com.borrowly.model.item;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Table(name = "item_images")
@Entity
@ToString(exclude = {"imageData", "item"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage {
    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(columnDefinition = "BYTEA", nullable = false)
    @Size(min = 1)
    private byte[] imageData;

    @NotBlank
    @Size(max = 255)
    @Column(length = 255, nullable = false)
    private String fileName;

    @NotBlank
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String contentType;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @NotNull
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
}
