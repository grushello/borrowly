package com.borrowly.mapper;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.*;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {
    UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapperImpl();
    }

    @Test
    void toResponse_MapsPublicFields(){
        User user = User.register("Mario", "Rossi", "mario@example.com", "TOP_SECRET_HASH");
        UserResponse response = mapper.toResponse(user);

        assertEquals(user.getId(), response.id());
        assertEquals("Mario", response.firstName());
        assertEquals("Rossi", response.lastName());
        assertEquals("mario@example.com", response.email());
        assertThat(response.currentBalance()).isEqualByComparingTo(BigDecimal.ZERO);    }

    @Test
    void toSummary_StripsExtraFields(){
        User user = User.register("Mario", "Rossi", "mario@example.com", "TOP_SECRET_HASH");
        UserSummaryResponse response = mapper.toSummary(user);

        assertEquals(user.getId(), response.id());
        assertEquals("Mario", response.firstName());
        assertEquals("Rossi", response.lastName());
    }

    @Test
    void updateEntity_PartialUpdate_AppliesProvidedFieldsOnly(){
        User user = User.register("Mario", "Rossi", "mario@example.com", "TOP_SECRET_HASH");
        UpdateUserRequest request = new UpdateUserRequest("Luigi", null, "+39 999999999");

        mapper.updateEntity(user, request);

        // Fields present in the request are applied
        assertEquals("Luigi", user.getFirstName());
        assertEquals("+39 999999999", user.getPhone());

        // Null fields in the request do not overwrite existing values
        assertEquals("Rossi", user.getLastName());
    }

    @Test
    void updateEntity_NeverTouchesPrivilegedOrImmutableFields() {
        User user = User.register("Mario", "Rossi", "mario@example.com", "TOP_SECRET_HASH");
        UUID originalId = user.getId();
        UpdateUserRequest request = new UpdateUserRequest("Luigi", null, "+39 999999999");

        mapper.updateEntity(user, request);
        
        assertEquals(originalId, user.getId());
        assertEquals("TOP_SECRET_HASH", user.getPasswordHash());
        assertEquals(UserRole.USER, user.getRole());
        assertThat(user.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertEquals(Boolean.TRUE, user.getEnabled());
        assertEquals("mario@example.com", user.getEmail());
    }
    @Test
    void toProfile_MapsAllFields() {
        User user = User.register(
                "Mario",
                "Rossi",
                "mario@example.com",
                "TOP_SECRET_HASH"
        );

        ItemSummaryResponse item =
                new ItemSummaryResponse(
                        UUID.randomUUID(),
                        "Bike",
                        BigDecimal.TEN,
                        null,
                        null,
                        "Mario Rossi",
                        null
                );

        ReviewResponse review =
                new ReviewResponse(
                        UUID.randomUUID(),
                        null,
                        null,
                        5,
                        "Excellent",
                        UUID.randomUUID(),
                        user.getCreatedAt()
                );

        UserProfileResponse profile = mapper.toProfile(
                user,
                List.of(item),
                List.of(review),
                4.8,
                12L
        );

        assertEquals(user.getId(), profile.id());
        assertEquals("Mario", profile.firstName());
        assertEquals("Rossi", profile.lastName());
        assertEquals(user.getCreatedAt(), profile.createdAt());

        assertThat(profile.items()).containsExactly(item);
        assertThat(profile.reviews()).containsExactly(review);

        assertEquals(4.8, profile.averageRating());
        assertEquals(12L, profile.reviewCount());
    }
}
