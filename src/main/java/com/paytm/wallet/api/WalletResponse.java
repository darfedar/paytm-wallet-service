package com.paytm.wallet.api;

import com.paytm.wallet.domain.Wallet;

public record WalletResponse(
        Long walletId,
        String userId,
        long balancePaise
) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(
                w.getId(),
                w.getUserId(),
                w.getBalancePaise()
        );
    }
}
