package com.borrowly.controller;

import com.borrowly.dto.request.TopUpRequest;
import com.borrowly.dto.request.WithdrawRequest;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/top-up")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse topUp(@Valid @RequestBody TopUpRequest request) {
        return transactionService.topUp(request);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(@Valid @RequestBody WithdrawRequest request) {
        return transactionService.withdraw(request);
    }

    @GetMapping
    public Page<TransactionResponse> getHistory(
            @RequestParam(required = false) List<TransactionType> type,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.getHistory(type, pageable);
    }
}