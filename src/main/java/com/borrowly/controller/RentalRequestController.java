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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rental-requests")
@RequiredArgsConstructor
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    @PostMapping
    public ResponseEntity<RentalRequestResponse> create(
            @Valid @RequestBody CreateRentalRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rentalRequestService.create(request));
    }

    @GetMapping("/incoming")
    public ResponseEntity<Page<RentalRequestResponse>> getIncoming(
            @RequestParam(required = false) RentalRequestStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(rentalRequestService.getIncoming(status, pageable));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<Page<RentalRequestResponse>> getOutgoing(
            @RequestParam(required = false) RentalRequestStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(rentalRequestService.getOutgoing(status, pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RentalResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalRequestService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RentalRequestResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalRequestService.reject(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RentalRequestResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(rentalRequestService.cancel(id));
    }
}