package com.borrowly.controller.page;

import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.service.rental.RentalService;
import com.borrowly.service.rentalrequest.RentalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RentalRequestsPageController {
    
    private final RentalRequestService rentalRequestService;

    @GetMapping("/rental-requests")
    public String getRequests(Model model) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate"));


        Page<RentalRequestResponse> pendingIncomingRentalRequests = rentalRequestService.getIncoming(RentalRequestStatus.PENDING, pageable);
        Page<RentalRequestResponse> pendingOutgoingRentalRequests = rentalRequestService.getOutgoing(RentalRequestStatus.PENDING, pageable);

        model.addAttribute("pendingIncoming", pendingIncomingRentalRequests);
        model.addAttribute("pendingOutgoing", pendingOutgoingRentalRequests);

        return "rental-requests";
    }

}
