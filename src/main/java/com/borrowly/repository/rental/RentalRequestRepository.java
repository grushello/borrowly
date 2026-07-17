package com.borrowly.repository.rental;

import com.borrowly.model.rental.RentalRequest;
import com.borrowly.model.rental.RentalRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RentalRequestRepository extends JpaRepository<RentalRequest, UUID> {

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByBorrower_Id(UUID borrowerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByItem_Owner_Id(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByItem_Owner_IdAndStatus(UUID ownerId,
                                                     RentalRequestStatus status,
                                                     Pageable pageable);

    boolean existsByItem_IdAndBorrower_IdAndStatusIn(UUID itemId,
                                                     UUID borrowerId,
                                                     Collection<RentalRequestStatus> statuses);

    @Query("""
            select case when count(rr) > 0 then true else false end
            from RentalRequest rr
            where rr.item.id = :itemId
              and rr.status = com.borrowly.model.rental.RentalRequestStatus.APPROVED
              and rr.startDate <= :endDate
              and rr.endDate   >= :startDate
            """)
    boolean existsOverlappingApproved(@Param("itemId") UUID itemId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByBorrower_IdAndStatus(UUID borrowerId,
                                                   RentalRequestStatus status,
                                                   Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    List<RentalRequest> findByItem_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID itemId,
            RentalRequestStatus status,
            LocalDate endDate,
            LocalDate startDate);
}