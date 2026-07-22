package com.borrowly.service.admin;

import com.borrowly.dto.response.AdminStatsResponse;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.ADMIN),
                userRepository.countByEnabledFalse(),
                itemRepository.count(),
                itemRepository.countByStatus(ItemStatus.ACTIVE),
                itemRepository.countByStatus(ItemStatus.RENTED),
                itemRepository.countByStatus(ItemStatus.ARCHIVED),
                categoryRepository.count()
        );
    }
}
