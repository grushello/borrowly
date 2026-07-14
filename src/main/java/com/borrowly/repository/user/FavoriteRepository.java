package com.borrowly.repository.user;

import com.borrowly.model.user.Favorite;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Page<Favorite> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUser_IdAndItem_Id(UUID userId, UUID itemId);

    @Modifying
    @Transactional
    int deleteByUser_IdAndItem_Id(UUID userId, UUID itemId);
}
