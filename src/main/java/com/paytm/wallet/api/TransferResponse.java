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
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromWalletId(),
                transfer.getToWalletId(),
                transfer.getAmountPaise(),
                transfer.getIdempotencyKey(),
                transfer.getStatus().name(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
