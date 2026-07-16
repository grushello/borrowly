package com.borrowly.security;

import com.borrowly.model.user.User;


public interface CurrentUserProvider {

    User getCurrentUser();
}