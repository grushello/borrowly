package com.borrowly.controller;

import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.service.rental.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final RentalService rentalService;

    @GetMapping("/as-borrower")
    public Page<RentalResponse> listAsBorrower(
            @RequestParam(required = false) List<RentalStatus> status,
            Pageable pageable
    ) {
        return rentalService.listAsBorrower(status, newestFirst(pageable));
    }

    @GetMapping("/as-owner")
    public Page<RentalResponse> listAsOwner(
            @RequestParam(required = false) List<RentalStatus> status,
            Pageable pageable
    ) {
        return rentalService.listAsOwner(status, newestFirst(pageable));
    }

    @GetMapping("/{id}")
    public RentalResponse getById(@PathVariable UUID id) {
        return rentalService.getById(id);
    }

    @PatchMapping("/{id}/return")
    public RentalResponse returnRental(@PathVariable UUID id) {
        return rentalService.returnRental(id);
    }

    private static Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }
}
