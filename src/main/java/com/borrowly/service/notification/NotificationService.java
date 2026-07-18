package com.borrowly.service.notification;

import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // Internal: called by the rental, transaction and scheduler flows, not by a controller.
    // rental and transaction are context links and may be null depending on the event.
    void send(User recipient,
              NotificationType type,
              String message,
              Rental rental,
              Transaction transaction);

    Page<NotificationResponse> listForCurrentUser(Pageable pageable);
}
