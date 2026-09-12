package com.paytm.wallet.api;

import com.paytm.wallet.domain.Transfer;

import java.time.Instant;

public record TransferResponse(
        Long transferId,
        Long from,
        Long to,
        long amountPaise,
        String idempotencyKey,
        String status,
        Instant createdAt,
        Instant completedAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getFromWalletId(),
                t.getToWalletId(),
                t.getAmountPaise(),
                t.getIdempotencyKey(),
                t.getStatus().name(),
                t.getCreatedAt(),
                t.getCompletedAt()
        );
    }
}
