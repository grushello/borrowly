package com.borrowly.controller.page;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.AdminStatsResponse;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.user.UserRole;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.admin.AdminStatsService;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.item.CategoryService;
import com.borrowly.service.item.ItemService;
import com.borrowly.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminPageController.class)
@Import({
        SecurityConfig.class,
        AuthTokenFilter.class,
        AuthEntryPointJwt.class,
        GlobalExceptionHandler.class
})
class AdminPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID ITEM_ID = UUID.randomUUID();
    private final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Pageable pageable = PageRequest.of(0, 10);

        UserResponse user = new UserResponse(
                USER_ID,
                "Alice",
                "Smith",
                "alice@example.com",
                "+37061234567",
                UserRole.ADMIN,
                BigDecimal.ZERO,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ItemSummaryResponse item = new ItemSummaryResponse(
                ITEM_ID,
                "Drill",
                BigDecimal.TEN,
                ItemCondition.GOOD,
                ItemStatus.ACTIVE,
                "Alice Smith",
                null
        );

        CategoryResponse category =
                new CategoryResponse(CATEGORY_ID, "Tools", "Power tools");

        Page<UserResponse> usersPage = new PageImpl<>(List.of(user), pageable, 1);
        Page<ItemSummaryResponse> itemsPage = new PageImpl<>(List.of(item), pageable, 1);

        when(categoryService.findAll()).thenReturn(List.of(category));
        when(userService.listUsers(any(Pageable.class))).thenReturn(usersPage);
        when(itemService.adminListItems(any(Pageable.class))).thenReturn(itemsPage);
        when(adminStatsService.getStats())
                .thenReturn(new AdminStatsResponse(1, 1, 0, 1, 1, 0, 0, 1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin returns admin page with model attributes")
    void getAdminPage() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("usersPage"))
                .andExpect(model().attributeExists("itemsPage"))
                .andExpect(model().attributeExists("stats"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin clamps negative page numbers to zero")
    void getAdminPageWithNegativePage() throws Exception {
        mockMvc.perform(get("/admin").param("usersPage", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin is forbidden for non-admin users")
    void getAdminPageAsUser() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }
}