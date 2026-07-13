package com.borrowly.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash"})
public class User {

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

    @Setter
    @NotBlank
    @Email
    @Column(unique=true, nullable = false)
    private String email;

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

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static User register(String firstName, String lastName,
                                String email, String passwordHash) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordHash)
                .build();
    }
}
