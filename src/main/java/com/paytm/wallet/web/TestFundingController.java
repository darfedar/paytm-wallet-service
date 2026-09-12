package com.paytm.wallet.web;

import com.paytm.wallet.api.WalletResponse;
import com.paytm.wallet.service.WalletService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/wallets")
@Profile("local")
public class TestFundingController {
    private final WalletService walletService;

    public TestFundingController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{id}/fund")
    public WalletResponse fund(@PathVariable Long id, @RequestParam long amountPaise) {
        return WalletResponse.from(walletService.fundForTest(id, amountPaise));
    }
}
