package com.borrowly.repository.rental;

import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
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
public interface RentalRepository extends JpaRepository<Rental, UUID> {

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<Rental> findByBorrowerIdAndStatusIn(UUID borrowerId,
                                             Collection<RentalStatus> statuses,
                                             Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    Page<Rental> findByBorrowerId(UUID borrowerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    @Query("""
            select r
            from Rental r
            where r.item.owner.id = :ownerId
              and r.status in :statuses
            """)
    Page<Rental> findByOwnerIdAndStatusIn(@Param("ownerId") UUID ownerId,
                                          @Param("statuses") Collection<RentalStatus> statuses,
                                          Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    @Query("""
            select r
            from Rental r
            where r.item.owner.id = :ownerId
            """)
    Page<Rental> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    List<Rental> findByEndDateBeforeAndStatus(LocalDate cutoff, RentalStatus status);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Rental r
            where r.item.id = :itemId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate   >= :startDate
            """)
    boolean existsOverlappingByStatuses(@Param("itemId") UUID itemId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("statuses") Collection<RentalStatus> statuses);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Rental r
            where r.item.id = :itemId
              and r.id <> :excludeRentalId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate   >= :startDate
            """)
    boolean existsOverlappingByStatusesExcluding(@Param("itemId") UUID itemId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("excludeRentalId") UUID excludeRentalId,
                                                 @Param("statuses") Collection<RentalStatus> statuses);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Rental r
            where r.item.id = :itemId
              and r.status in :statuses
            """)
    boolean existsByItemIdAndStatusIn(@Param("itemId") UUID itemId,
                                      @Param("statuses") Collection<RentalStatus> statuses);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    @Query("""
            select r
            from Rental r
            where r.item.id = :itemId
              and r.borrower.id = :borrowerId
              and r.startDate = :startDate
              and r.endDate = :endDate
            """)
    Optional<Rental> findByItemIdAndBorrowerIdAndStartDateAndEndDate(@Param("itemId") UUID itemId,
                                                                     @Param("borrowerId") UUID borrowerId,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"item", "item.owner", "borrower"})
    List<Rental> findByEndDateBeforeAndStatusIn(LocalDate cutoff, Collection<RentalStatus> statuses);
}