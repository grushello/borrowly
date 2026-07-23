package com.borrowly.controller.advice;

import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalUiControllerAdvice {

    private final Optional<NotificationService> notificationService;

    @ModelAttribute("notifications")
    public List<NotificationResponse> globalNotifications(Principal principal, HttpServletRequest request) {

        if (request.getRequestURI().startsWith("/api/")) {
            return Collections.emptyList();
        }

        if (principal == null || notificationService.isEmpty()) {
            return Collections.emptyList();
        }

        Pageable topFive = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationService.get().listForCurrentUser(topFive).getContent();
    }
}