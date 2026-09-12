package com.paytm.wallet.api;

import com.paytm.wallet.domain.Wallet;

public record WalletResponse(
        Long walletId,
        String userId,
        long balancePaise
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalancePaise()
        );
    }
}
