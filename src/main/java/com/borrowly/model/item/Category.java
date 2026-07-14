package com.borrowly.model.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Table(name = "categories")
@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Category {
    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Setter
    @NotBlank
    @Size(max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Setter
    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /**
     * Custom Constructor for the Builder.
     */
    @Builder(access = AccessLevel.PACKAGE)
    public Category(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
    }
}
