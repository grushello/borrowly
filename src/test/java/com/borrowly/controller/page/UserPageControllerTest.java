package com.borrowly.controller.page;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.UserProfileResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserPageController.class)
@Import({
        SecurityConfig.class,
        AuthTokenFilter.class,
        AuthEntryPointJwt.class,
        GlobalExceptionHandler.class
})
class UserPageControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private UserService userService;


    @MockitoBean
    private CurrentUserProvider currentUserProvider;


    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UUID USER_ID = UUID.randomUUID();



    @Test
    @WithMockUser
    @DisplayName("GET /user/{id} returns profile page")
    void getUserProfilePage() throws Exception {


        UserProfileResponse profile =
                new UserProfileResponse(
                        USER_ID,
                        "Alice",
                        "Smith",
                        LocalDateTime.now(),
                        List.of(),
                        List.of(),
                        5.0,
                        1
                );


        when(userService.getUserProfile(USER_ID))
                .thenReturn(profile);


        when(currentUserProvider.getCurrentUserOptional())
                .thenReturn(Optional.empty());


        mockMvc.perform(get("/user/{id}", USER_ID))

                .andExpect(status().isOk())

                .andExpect(view().name("user/profile"))

                .andExpect(model().attributeExists("profile"))

                .andExpect(model().attribute("isOwner", false));
    }



    @Test
    @WithMockUser
    @DisplayName("GET /user/{id} detects owner")
    void getUserProfilePageOwner() throws Exception {


        User user = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "password"
        );


        UserProfileResponse profile =
                new UserProfileResponse(
                        USER_ID,
                        "Alice",
                        "Smith",
                        LocalDateTime.now(),
                        List.of(),
                        List.of(),
                        0.0,
                        0
                );


        when(userService.getUserProfile(USER_ID))
                .thenReturn(profile);


        when(currentUserProvider.getCurrentUserOptional())
                .thenReturn(Optional.of(user));


        // if your User id is generated, set it:
        org.springframework.test.util.ReflectionTestUtils
                .setField(user, "id", USER_ID);



        mockMvc.perform(get("/user/{id}", USER_ID))

                .andExpect(status().isOk())

                .andExpect(view().name("user/profile"))

                .andExpect(model().attribute("isOwner", true));
    }



    @Test
    @WithMockUser
    @DisplayName("GET /account returns account page")
    void getAccountPage() throws Exception {


        UserResponse account =
                new UserResponse(
                        USER_ID,
                        "Alice",
                        "Smith",
                        "alice@example.com",
                        "+37061234567",
                        UserRole.USER,
                        BigDecimal.ZERO,
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );


        when(userService.getAccountInfo())
                .thenReturn(account);



        mockMvc.perform(get("/account"))

                .andExpect(status().isOk())

                .andExpect(view().name("user/account"))

                .andExpect(model().attributeExists("account"));
    }



    @Test
    @WithMockUser
    @DisplayName("GET /profile redirects to current user profile")
    void redirectToMyProfile() throws Exception {


        User user = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "password"
        );


        org.springframework.test.util.ReflectionTestUtils
                .setField(user, "id", USER_ID);



        when(currentUserProvider.getCurrentUser())
                .thenReturn(user);



        mockMvc.perform(get("/profile"))

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/user/" + USER_ID));
    }
}