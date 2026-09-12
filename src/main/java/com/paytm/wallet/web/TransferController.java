package com.paytm.wallet.web;

import com.paytm.wallet.api.*;
import com.paytm.wallet.domain.Wallet;
import com.paytm.wallet.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transfers;
    private final WalletService wallets;

    public TransferController(TransferService transferService, WalletService walletService) {
        transfers = transferService;
        wallets = walletService;
    }

    @PostMapping
    public TransferResponse create(@RequestHeader("Authorization") String auth, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest r) {
        Wallet from = wallets.get(r.from());
        if (!from.getUserId().equals(WalletController.user(auth)))
            throw new IllegalArgumentException("Caller is not the owner of the source wallet");
        return TransferResponse.from(transfers.transfer(r, key));
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) {
        return TransferResponse.from(transfers.get(id));
    }
}
