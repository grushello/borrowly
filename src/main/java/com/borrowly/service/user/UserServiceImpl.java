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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse disableUser(UUID id) {
        User target = getUserOrThrow(id);

        if (target.getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new CannotDisableSelfException();
        }

        target.setEnabled(false);
        log.info("Disabled user '{}'", target.getId());
        return userMapper.toResponse(target);
    }

    @Override
    @Transactional
    public UserResponse enableUser(UUID id) {
        User target = getUserOrThrow(id);
        target.setEnabled(true);
        log.info("Enabled user '{}'", target.getId());
        return userMapper.toResponse(target);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        User user = currentUserProvider.getCurrentUser();
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {
        User user = currentUserProvider.getCurrentUser();
        userMapper.updateEntity(user, request);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(UUID id) {
        User user = getUserOrThrow(id);
        return userMapper.toSummary(user);
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}