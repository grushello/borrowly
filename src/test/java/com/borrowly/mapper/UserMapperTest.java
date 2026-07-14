package com.borrowly.mapper;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

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
        assertEquals(new BigDecimal("0"), response.currentBalance());
    }

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

        // Regression guard: UpdateUserRequest has no way to reach these fields today,
        // but if one is ever added without a matching @Mapping(..., ignore = true),
        // this test should catch it.
        assertEquals(originalId, user.getId());
        assertEquals("TOP_SECRET_HASH", user.getPasswordHash());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(new BigDecimal("0"), user.getCurrentBalance());
        assertEquals(Boolean.TRUE, user.getEnabled());
        assertEquals("mario@example.com", user.getEmail());
    }
}
