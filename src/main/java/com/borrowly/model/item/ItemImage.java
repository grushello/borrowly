package com.borrowly.model.item;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "item_images")
@Entity
@ToString(exclude = {"imageData"})
public class ItemImage {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(columnDefinition = "BYTEA", nullable = false)
    private byte[] imageData;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    @Size(max = 100)
    private String contentType;

    @NotNull
    @Builder.Default
    private Boolean primaryImage = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Item item;
}
