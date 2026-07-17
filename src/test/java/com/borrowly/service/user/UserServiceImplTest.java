package com.borrowly.service.user;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.exception.CannotDisableSelfException;
import com.borrowly.exception.UserNotFoundException;
import com.borrowly.mapper.UserMapper;
import com.borrowly.model.user.User;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserServiceImpl userService;

    private User userWithId(UUID id) {
        User user = User.register("Alice", "Smith", "alice@example.com", "hashed");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserResponse responseFor(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhone(), user.getRole(), BigDecimal.ZERO,
                user.getEnabled(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("listUsers maps every page element through the mapper")
    void listUsersMapsPage() {
        User alice = userWithId(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(alice), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toResponse(alice)).thenReturn(responseFor(alice));

        Page<UserResponse> result = userService.listUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("alice@example.com");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isZero();
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("disableUser sets enabled=false for another user")
    void disableUserSetsEnabledFalse() {
        UUID targetId = UUID.randomUUID();
        User target = userWithId(targetId);
        User admin = userWithId(UUID.randomUUID());
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(currentUserProvider.getCurrentUser()).thenReturn(admin);

        when(userMapper.toResponse(target)).thenAnswer(inv -> responseFor(target));

        UserResponse response = userService.disableUser(targetId);

        assertThat(target.getEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("disableUser throws 409 when an admin targets their own account")
    void disableUserRejectsSelf() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(currentUserProvider.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> userService.disableUser(adminId))
                .isInstanceOf(CannotDisableSelfException.class);

        assertThat(admin.getEnabled()).isTrue();
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("disableUser throws 404 when the target does not exist")
    void disableUserThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(id))
                .isInstanceOf(UserNotFoundException.class);

        verify(currentUserProvider, never()).getCurrentUser();
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("enableUser sets enabled=true")
    void enableUserSetsEnabledTrue() {
        UUID targetId = UUID.randomUUID();
        User target = userWithId(targetId);
        target.setEnabled(false);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userMapper.toResponse(target)).thenAnswer(inv -> responseFor(target));

        UserResponse response = userService.enableUser(targetId);

        assertThat(target.getEnabled()).isTrue();
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("enableUser throws 404 when the target does not exist")
    void enableUserThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.enableUser(id))
                .isInstanceOf(UserNotFoundException.class);

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("enableUser does not consult the current user (no self-check)")
    void enableUserDoesNotCheckSelf() {
        UUID targetId = UUID.randomUUID();
        User target = userWithId(targetId);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userMapper.toResponse(target)).thenAnswer(inv -> responseFor(target));

        userService.enableUser(targetId);

        verify(currentUserProvider, never()).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile returns the authenticated user's full profile")
    void getProfileReturnsCurrentUser() {
        User user = userWithId(UUID.randomUUID());
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(responseFor(user));

        UserResponse result = userService.getProfile();

        assertThat(result.email()).isEqualTo("alice@example.com");
        verify(currentUserProvider).getCurrentUser();
        verify(userMapper).toResponse(user);
    }

    @Test
    @DisplayName("updateProfile applies only provided fields, leaves others untouched")
    void updateProfilePartialFields() {
        User user = userWithId(UUID.randomUUID());
        user.setPhone("+37061234567");
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(responseFor(user));

        UpdateUserRequest request = new UpdateUserRequest("Bob", null, null);
        userService.updateProfile(request);

        verify(userMapper).updateEntity(user, request);
        verify(userMapper).toResponse(user);
        assertThat(user.getPhone()).isEqualTo("+37061234567");
    }

    @Test
    @DisplayName("getUserSummary returns public summary for existing user")
    void getUserSummaryReturnsData() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        UserSummaryResponse summary = new UserSummaryResponse(id, "Alice", "Smith");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toSummary(user)).thenReturn(summary);

        UserSummaryResponse result = userService.getUserSummary(id);

        assertThat(result.firstName()).isEqualTo("Alice");
        verify(userMapper).toSummary(user);
    }

    @Test
    @DisplayName("getUserSummary throws 404 when user does not exist")
    void getUserSummaryThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserSummary(id))
                .isInstanceOf(UserNotFoundException.class);

        verify(userMapper, never()).toSummary(any());
    }
}