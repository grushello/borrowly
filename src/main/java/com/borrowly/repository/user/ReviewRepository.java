package com.borrowly.repository.user;

import com.borrowly.model.user.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"reviewer", "rental"})
    Page<Review> findByRental_Item_IdOrderByCreatedAtDesc(UUID itemId, Pageable pageable);

    @EntityGraph(attributePaths = {"reviewer", "rental"})
    Page<Review> findByReviewer_Id(UUID reviewerId, Pageable pageable);

    long countByRental_Item_Id(UUID itemId);

    @Query("""
            select avg(r.rating)
            from Review r
            where r.rental.item.id = :itemId
            """)
    Double averageRatingByItemId(@Param("itemId") UUID itemId);

    boolean existsByRental_Id(UUID rentalId);
}