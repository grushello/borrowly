package com.borrowly.repository.user;

import com.borrowly.model.user.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndItemId(UUID userId, UUID itemId);

    @Modifying
    @Query("""
            delete from Favorite f
            where f.user.id = :userId
              and f.item.id = :itemId
            """)
    int deleteByUserIdAndItemId(@Param("userId") UUID userId, @Param("itemId") UUID itemId);

    Optional<Favorite> findByUserIdAndItemId(UUID userId, UUID itemId);
}
