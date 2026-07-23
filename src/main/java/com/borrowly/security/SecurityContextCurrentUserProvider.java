package com.borrowly.security;

import com.borrowly.exception.AuthUserNotFoundException;
import com.borrowly.model.user.User;
import com.borrowly.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthUserNotFoundException("No authenticated user in the security context");
        }
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthUserNotFoundException(
                        "Authenticated user not found: " + email));
    }

    @Override
    public Optional<User> getCurrentUserOptional() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(
                authentication.getName()
        );
    }

}