package com.borrowly.scheduler;

import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueRentalScheduler {

    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueRentals() {

        LocalDate today = LocalDate.now();

        List<Rental> overdueRentals =
                rentalRepository.findByEndDateBeforeAndStatusIn(
                        today,
                        List.of(
                                RentalStatus.ACTIVE,
                                RentalStatus.OVERDUE
                        )
                );

        for (Rental rental : overdueRentals) {
            try {
                requiresNewTransactionTemplate.executeWithoutResult(status ->
                        processRental(rental.getId(), today)
                );
            } catch (Exception ex) {
                log.error(
                        "Failed processing overdue rental {}",
                        rental.getId(),
                        ex
                );
            }
        }
    }

    private void processRental(UUID rentalId, LocalDate today) {

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() ->
                        new RentalNotFoundException(rentalId)
                );

        if (rental.getStatus() == RentalStatus.ACTIVE) {
            rental.markOverdue();
            rentalRepository.save(rental);
        }

        long overdueDays = ChronoUnit.DAYS.between(
                rental.getEndDate(),
                today
        );

        String message = String.format(
                "Your rental of '%s' is overdue by %d day%s.",
                rental.getItem().getTitle(),
                overdueDays,
                overdueDays == 1 ? "" : "s"
        );

        try {
            notificationService.send(
                    rental.getBorrower(),
                    NotificationType.RENTAL_OVERDUE,
                    message,
                    rental,
                    null
            );
        } catch (Exception ex) {
            log.error(
                    "Failed sending overdue notification for rental {}",
                    rental.getId(),
                    ex
            );
        }
    }
}