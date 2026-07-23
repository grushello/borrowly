package com.borrowly.controller.page;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.item.ItemImageService;
import com.borrowly.service.item.ItemService;
import com.borrowly.service.rentalrequest.RentalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ItemPageController {
    private final ItemService itemService;
    private final FavoriteService favoriteService;
    private final CurrentUserProvider currentUserProvider;
    private final ItemImageService itemImageService;
    private final RentalRequestService rentalRequestService;

    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") UUID id, Model model, Principal principal) {
        ItemResponse item = itemService.getById(id);

        boolean isFavorite = false;
        boolean isOwner = false;
        if (principal != null) {
            isFavorite = favoriteService.isFavoritedByCurrentUser(id);
            UUID currentUserId = currentUserProvider.getCurrentUser().getId();
            if (currentUserId != null){
                isOwner = currentUserId.equals(item.owner().id());
            }
        }

        List<ItemImageResponse> imagesMetadata = itemImageService.listMetadata(id);

        Optional<UUID> pendingRequestIdOpt = rentalRequestService.getPendingRentalRequestId(item.id(), currentUserProvider.getCurrentUser().getId());

        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("item", item);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("images", imagesMetadata);

        if (pendingRequestIdOpt.isPresent()) {
            model.addAttribute("itemRentalRequestPendingForUser", true);
            model.addAttribute("pendingRentalRequestId", pendingRequestIdOpt.get());
        } else {
            model.addAttribute("itemRentalRequestPendingForUser", false);
        }
        return "item";
    }
}
