package com.borrowly.controller;

import com.borrowly.dto.request.CreateRentalRequest;
import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.service.rentalrequest.RentalRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/rental-requests")
@RequiredArgsConstructor
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalRequestResponse create(@Valid @RequestBody CreateRentalRequest request) {
        return rentalRequestService.create(request);
    }

    @GetMapping("/incoming")
    public Page<RentalRequestResponse> getIncoming(
            @RequestParam(required = false) RentalRequestStatus status,
            Pageable pageable) {
        return rentalRequestService.getIncoming(status, pageable);
    }

    @GetMapping("/outgoing")
    public Page<RentalRequestResponse> getOutgoing(
            @RequestParam(required = false) RentalRequestStatus status,
            Pageable pageable) {
        return rentalRequestService.getOutgoing(status, pageable);
    }

    @PatchMapping("/{id}/approve")
    public RentalResponse approve(@PathVariable UUID id) {
        return rentalRequestService.approve(id);
    }

    @PatchMapping("/{id}/reject")
    public RentalRequestResponse reject(@PathVariable UUID id) {
        return rentalRequestService.reject(id);
    }

    @PatchMapping("/{id}/cancel")
    public RentalRequestResponse cancel(@PathVariable UUID id) {
        return rentalRequestService.cancel(id);
    }
}