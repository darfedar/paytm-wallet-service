package com.paytm.wallet.web;

import com.paytm.wallet.api.WalletResponse;
import com.paytm.wallet.service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final WalletService service;

    public WalletController(WalletService s) {
        service = s;
    }

    @PostMapping
    public WalletResponse create(@RequestHeader("Authorization") String auth) {
        return WalletResponse.from(service.getOrCreate(user(auth)));
    }

    @GetMapping("/{id}")
    public WalletResponse get(@PathVariable Long id) {
        return WalletResponse.from(service.get(id));
    }

    static String user(String a) {
        if (a == null || !a.startsWith("Bearer "))
            throw new IllegalArgumentException("Authorization must be a Bearer token");
        String u = a.substring(7).trim();
        if (u.isBlank()) throw new IllegalArgumentException("Bearer token is empty");
        return u;
    }
}
