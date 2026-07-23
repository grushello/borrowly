package com.borrowly.repository.transaction;

import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @EntityGraph(attributePaths = "rental")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "rental")
    Page<Transaction> findByUserIdAndTypeIn(UUID userId,
                                            Collection<TransactionType> types,
                                            Pageable pageable);
}