package com.borrowly.service.notification;

import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.user.User;
import com.borrowly.support.AbstractPostgresTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationServiceTransactionTest extends AbstractPostgresTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void sendOutsideTransactionIsRejected() {
        User recipient = User.register("Alice", "Smith", "alice@example.com", "hashed");

        assertThatThrownBy(() -> notificationService.send(
                recipient, NotificationType.GENERAL, "Anything", null, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
