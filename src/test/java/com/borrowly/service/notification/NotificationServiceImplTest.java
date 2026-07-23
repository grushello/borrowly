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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient() {
        return User.register("Alice", "Smith", "alice@example.com", "hashed");
    }

    private Notification captureSaved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("send persists a notification for the given recipient")
    void sendPersistsNotification() {
        User alice = recipient();

        notificationService.send(alice, NotificationType.RENTAL_APPROVED,
                "Your request for 'Drill' was approved", null, null);

        Notification saved = captureSaved();
        assertThat(saved.getRecipient()).isSameAs(alice);
        assertThat(saved.getType()).isEqualTo(NotificationType.RENTAL_APPROVED);
        assertThat(saved.getMessage()).isEqualTo("Your request for 'Drill' was approved");
    }

    @Test
    @DisplayName("send accepts a null rental and transaction")
    void sendAcceptsNullContext() {
        notificationService.send(recipient(), NotificationType.RENTAL_REQUEST,
                "New request for 'Drill'", null, null);

        Notification saved = captureSaved();
        assertThat(saved.getRental()).isNull();
        assertThat(saved.getTransaction()).isNull();
    }

    @Test
    @DisplayName("send links both the rental and the transaction when supplied")
    void sendLinksRentalAndTransaction() {
        Rental rental = Rental.builder().build();
        Transaction transaction = Transaction.builder().build();

        notificationService.send(recipient(), NotificationType.FINE,
                "A fine of 15.00 was charged", rental, transaction);

        Notification saved = captureSaved();
        assertThat(saved.getRental()).isSameAs(rental);
        assertThat(saved.getTransaction()).isSameAs(transaction);
    }

    @Test
    @DisplayName("listForCurrentUser queries by the authenticated user's id")
    void listQueriesByCurrentUserId() {
        User alice = recipient();
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserProvider.getCurrentUser()).thenReturn(alice);
        when(notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(alice.getId(), pageable))
                .thenReturn(Page.empty(pageable));

        notificationService.listForCurrentUser(pageable);

        verify(notificationRepository)
                .findByRecipient_IdOrderByCreatedAtDesc(alice.getId(), pageable);
    }

    @Test
    @DisplayName("listForCurrentUser maps entities through the mapper")
    void listMapsToResponses() {
        User alice = recipient();
        Pageable pageable = PageRequest.of(0, 20);

        Notification notification = Notification.builder()
                .message("Your request for 'Drill' was approved")
                .type(NotificationType.RENTAL_APPROVED)
                .recipient(alice)
                .build();

        NotificationResponse response = new NotificationResponse(
                notification.getId(),
                "Your request for 'Drill' was approved",
                NotificationType.RENTAL_APPROVED,
                null,
                null,
                LocalDateTime.now());

        when(currentUserProvider.getCurrentUser()).thenReturn(alice);
        when(notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(alice.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        Page<NotificationResponse> page = notificationService.listForCurrentUser(pageable);

        assertThat(page.getContent()).containsExactly(response);
    }
}
