package com.borrowly.service.admin;

import com.borrowly.dto.response.AdminStatsResponse;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminStatsService adminStatsService;

    @Test
    @DisplayName("getStats aggregates counts from every repository")
    void getStatsAggregatesCounts() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.countByEnabledFalse()).thenReturn(3L);
        when(itemRepository.count()).thenReturn(40L);
        when(itemRepository.countByStatus(ItemStatus.ACTIVE)).thenReturn(25L);
        when(itemRepository.countByStatus(ItemStatus.RENTED)).thenReturn(10L);
        when(itemRepository.countByStatus(ItemStatus.ARCHIVED)).thenReturn(5L);
        when(categoryRepository.count()).thenReturn(7L);

        AdminStatsResponse stats = adminStatsService.getStats();

        assertThat(stats.totalUsers()).isEqualTo(12L);
        assertThat(stats.adminUsers()).isEqualTo(2L);
        assertThat(stats.disabledUsers()).isEqualTo(3L);
        assertThat(stats.totalItems()).isEqualTo(40L);
        assertThat(stats.activeItems()).isEqualTo(25L);
        assertThat(stats.rentedItems()).isEqualTo(10L);
        assertThat(stats.archivedItems()).isEqualTo(5L);
        assertThat(stats.totalCategories()).isEqualTo(7L);
    }

    @Test
    @DisplayName("getStats queries each item status separately")
    void getStatsQueriesEachStatus() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.countByEnabledFalse()).thenReturn(0L);
        when(itemRepository.count()).thenReturn(0L);
        when(itemRepository.countByStatus(ItemStatus.ACTIVE)).thenReturn(0L);
        when(itemRepository.countByStatus(ItemStatus.RENTED)).thenReturn(0L);
        when(itemRepository.countByStatus(ItemStatus.ARCHIVED)).thenReturn(0L);
        when(categoryRepository.count()).thenReturn(0L);

        adminStatsService.getStats();

        verify(userRepository).countByRole(UserRole.ADMIN);
        verify(itemRepository).countByStatus(ItemStatus.ACTIVE);
        verify(itemRepository).countByStatus(ItemStatus.RENTED);
        verify(itemRepository).countByStatus(ItemStatus.ARCHIVED);
    }
}