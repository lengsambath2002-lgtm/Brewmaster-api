package com.sambath.admincafe.transaction;

import com.sambath.admincafe.transaction.dto.RefundRequest;
import com.sambath.admincafe.transaction.dto.RefundResponse;
import com.sambath.admincafe.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public List<TransactionResponse> list() {
        return transactionService.findAll();
    }

    @PostMapping("/{id}/refund")
    public RefundResponse refund(
            @PathVariable String id,
            @Valid @RequestBody(required = false) RefundRequest request
    ) {
        return transactionService.refund(id, request);
    }
}
