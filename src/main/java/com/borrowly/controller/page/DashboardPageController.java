package com.borrowly.controller.page;

import com.borrowly.dto.response.*;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.item.ItemService;
import com.borrowly.service.rental.RentalService;
import com.borrowly.service.transaction.TransactionService;
import com.borrowly.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardPageController {
    private final FavoriteService favoriteService;
    private final ItemService itemService;
    private final UserService userService;
    private final RentalService rentalService;
    private final TransactionService transactionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        Pageable widgetPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FavoriteResponse> favoritesPage = favoriteService.listForCurrentUser(widgetPageable);
        Page<ItemSummaryResponse> itemsPage = itemService.getCurrentUserItems(widgetPageable);
        UserResponse userResponse = userService.getProfile();

        Pageable activeRentalPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "endDate"));

        Page<RentalResponse> activeRentalsAsBorrower = rentalService.listAsBorrower(List.of(RentalStatus.ACTIVE), activeRentalPageable);
        Page<RentalResponse> activeRentalsAsOwner = rentalService.listAsOwner(List.of(RentalStatus.ACTIVE), activeRentalPageable);

        Page<TransactionResponse> transactionsPage = transactionService.getHistory(null, widgetPageable);

        model.addAttribute("favorites", favoritesPage.getContent());
        model.addAttribute("userItemListings", itemsPage.getContent());
        model.addAttribute("user", userResponse);
        model.addAttribute("activeRentalsAsBorrower", activeRentalsAsBorrower);
        model.addAttribute("activeRentalsAsOwner", activeRentalsAsOwner);
        model.addAttribute("recentTransactions", transactionsPage.getContent());

        return "dashboard";
    }
}
