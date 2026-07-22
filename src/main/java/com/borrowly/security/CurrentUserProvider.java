package com.borrowly.security;

import com.borrowly.model.user.User;

import java.util.Optional;


public interface CurrentUserProvider {

    User getCurrentUser();

    Optional<User> getCurrentUserOptional();

}