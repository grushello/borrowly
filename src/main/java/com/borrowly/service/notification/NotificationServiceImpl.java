package com.borrowly.service.notification;

import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.mapper.NotificationMapper;
import com.borrowly.model.notification.Notification;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.user.User;
import com.borrowly.repository.notification.NotificationRepository;
import com.borrowly.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    // MANDATORY: a notification always belongs to the operation that raised it. If the
    // approve or the payout rolls back, the notification about it must go too.
    @Transactional(propagation = Propagation.MANDATORY)
    public void send(User recipient,
                     NotificationType type,
                     String message,
                     Rental rental,
                     Transaction transaction) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .rental(rental)
                .transaction(transaction)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForCurrentUser(Pageable pageable) {
        User recipient = currentUserProvider.getCurrentUser();

        return notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(recipient.getId(), pageable)
                .map(notificationMapper::toResponse);
    }
}
