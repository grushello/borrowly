package com.borrowly.model.user;

import com.borrowly.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Builder(access = AccessLevel.PACKAGE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"passwordHash"})
public class User extends BaseEntity {

    @jakarta.persistence.Id
    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Setter
    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Setter
    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Column(unique=true, nullable = false)
    private String email;
    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    @NotBlank
    private String passwordHash;

    @Setter
    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$")
    private String phone;

    @NotNull
    @Enumerated(STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @NotNull
    @DecimalMin("0.00")
    @Column(precision=14, scale=2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Setter
    @NotNull
    @Builder.Default
    private Boolean enabled = true;

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

    public static User register(String firstName, String lastName,
                                String email, String passwordHash) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizeEmail(email))
                .passwordHash(passwordHash)
                .build();
    }
    public void addBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
    }

    public void subtractBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
