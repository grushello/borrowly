package com.borrowly.controller;

import com.borrowly.dto.request.TopUpRequest;
import com.borrowly.dto.request.WithdrawRequest;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/top-up")
    public ResponseEntity<TransactionResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.topUp(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.withdraw(request));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @RequestParam(required = false) List<TransactionType> type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getHistory(type, page, size));
    }
}