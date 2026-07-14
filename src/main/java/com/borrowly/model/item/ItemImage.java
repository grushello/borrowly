package com.borrowly.model.item;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "item_images")
@Entity
@ToString(exclude = {"imageData"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemImage {
    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(columnDefinition = "BYTEA", nullable = false)
    private byte[] imageData;

    @NotBlank
    @Size(max = 255)
    @Column(length = 255)
    private String fileName;

    @NotBlank
    @Size(max = 100)
    @Column(length = 100)
    private String contentType;

    @NotNull
    private Boolean primaryImage = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Builder(access = AccessLevel.PACKAGE)
    public ItemImage(byte[] imageData, String fileName, String contentType, Boolean primaryImage, Item item) {
        this.imageData = imageData;
        this.fileName = fileName;
        this.contentType = contentType;
        this.primaryImage = (primaryImage != null) && primaryImage;
        this.item = item;
    }
}
