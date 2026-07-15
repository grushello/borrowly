package com.borrowly.repository.notification;

import com.borrowly.model.notification.Notification;
import com.borrowly.model.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipient_IdOrderByCreatedAtDesc(
            UUID recipientId,
            Pageable pageable
    );

    Page<Notification> findByRecipient_IdAndTypeOrderByCreatedAtDesc(
            UUID recipientId,
            NotificationType type,
            Pageable pageable
    );

    long countByRecipient_Id(UUID recipientId);
}