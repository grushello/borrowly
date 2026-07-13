package com.borrowly.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash"})
public class User {

    @jakarta.persistence.Id
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Column(unique=true, nullable = false)
    private String email;

    @NotBlank
    private String passwordHash;

    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$")
    private String phone;

    @NotNull
    @Enumerated(STRING)
    private UserRole role;

    @NotNull
    @DecimalMin("0.00")
    @Column(precision=14, scale=2)
    private BigDecimal currentBalance;

    @NotNull
    private Boolean enabled;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.currentBalance == null) {
            this.currentBalance = BigDecimal.ZERO;
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}
