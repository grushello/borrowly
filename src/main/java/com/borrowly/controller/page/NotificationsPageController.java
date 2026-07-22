package com.borrowly.controller.page;
import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationsPageController {
    private final NotificationService notificationService;

    @GetMapping
    public String getItemDetail(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        Page<NotificationResponse> notificationsPage = notificationService.listForCurrentUser(pageable);

        model.addAttribute("notificationsPage", notificationsPage);

        return "notifications";
    }
}
