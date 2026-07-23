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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalRequestRepository extends JpaRepository<RentalRequest, UUID> {

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByBorrowerId(UUID borrowerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<RentalRequest> findByBorrowerIdAndStatus(UUID borrowerId,
                                                  RentalRequestStatus status,
                                                  Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    @Query("""
            select rr
            from RentalRequest rr
            where rr.item.owner.id = :ownerId
            """)
    Page<RentalRequest> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    @Query("""
            select rr
            from RentalRequest rr
            where rr.item.owner.id = :ownerId
              and rr.status = :status
            """)
    Page<RentalRequest> findByOwnerIdAndStatus(@Param("ownerId") UUID ownerId,
                                               @Param("status") RentalRequestStatus status,
                                               Pageable pageable);

    boolean existsByItemIdAndBorrowerIdAndStatusIn(UUID itemId,
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
    @Query("""
            select rr
            from RentalRequest rr
            where rr.item.id = :itemId
              and rr.status = :status
              and rr.startDate <= :endDate
              and rr.endDate   >= :startDate
            """)
    List<RentalRequest> findOverlappingByItemIdAndStatus(@Param("itemId") UUID itemId,
                                                         @Param("status") RentalRequestStatus status,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    Optional<RentalRequest> findFirstByItemIdAndBorrowerIdAndStatusIn(
            UUID itemId,
            UUID borrowerId,
            Collection<RentalRequestStatus> statuses
    );
}