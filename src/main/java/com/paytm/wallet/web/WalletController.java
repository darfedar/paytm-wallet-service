package com.paytm.wallet.web;

import com.paytm.wallet.api.WalletResponse;
import com.paytm.wallet.service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public WalletResponse create(@RequestHeader("Authorization") String auth) {
        return WalletResponse.from(walletService.getOrCreate(user(auth)));
    }

    @GetMapping("/{id}")
    public WalletResponse get(@PathVariable Long id) {
        return WalletResponse.from(walletService.get(id));
    }

    static String user(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
            throw new IllegalArgumentException("Authorization must be a Bearer token");
        String userId = authorizationHeader.substring(7).trim();
        if (userId.isBlank())
            throw new IllegalArgumentException("Bearer token is empty");
        return userId;
    }
}
