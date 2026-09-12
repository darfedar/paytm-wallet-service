package com.paytm.wallet.service;

import com.paytm.wallet.api.TransferRequest;
import com.paytm.wallet.domain.*;
import com.paytm.wallet.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final WalletRepository wallets;
    private final TransferRepository transfers;
    private final Counter transfersCreated;
    private final Counter transfersDeclined;
    private final Counter idempotentReplays;

    public TransferService(WalletRepository wallet, TransferRepository transferRepository, MeterRegistry registry) {
        wallets = wallet;
        transfers = transferRepository;
        transfersCreated = registry.counter("transfers_created_total");
        transfersDeclined = registry.counter("transfers_declined_insufficient_funds_total");
        idempotentReplays = registry.counter("transfers_idempotent_replays_total");
    }

    @Transactional
    public Transfer transfer(TransferRequest transferRequest, String key) {
        validate(transferRequest, key);
        String hash = hash(transferRequest);
        int inserted = transfers.reserve(transferRequest.from(), transferRequest.to(), transferRequest.amountPaise(),
                key, hash);
        if (inserted == 0) {
            Transfer existing = transfers.findByIdempotencyKey(key).orElseThrow();
            if (!existing.getRequestHash().equals(hash))
                throw new IdempotencyConflict("Idempotency key reused with a different request");
            log.info("event=idempotent_replay transferId={} idempotencyKey={}", existing.getId(), key);
            idempotentReplays.increment();
            return existing;
        }
        long lowerWalletId = Math.min(transferRequest.from(), transferRequest.to()),
                higherWalletId = Math.max(transferRequest.from(), transferRequest.to());
        wallets.findByIdForUpdate(lowerWalletId)
                .orElseThrow(() -> new WalletService.NotFound("Wallet not found: " + lowerWalletId));
        wallets.findByIdForUpdate(higherWalletId)
                .orElseThrow(() -> new WalletService.NotFound("Wallet not found: " + higherWalletId));
        int debited = wallets.debitIfSufficient(transferRequest.from(), transferRequest.amountPaise());
        Transfer transfer = transfers.findByIdempotencyKey(key).orElseThrow();

        log.info("event=transfer_created transferId={} from={} to={} amountPaise={}", transfer.getId(),
                transferRequest.from(), transferRequest.to(), transferRequest.amountPaise());
        transfersCreated.increment();

        if (debited == 0) {
            transfer.decline();
            log.info("event=transfer_declined_insufficient_funds transferId={} from={} to={} amountPaise={}",
                    transfer.getId(), transferRequest.from(), transferRequest.to(), transferRequest.amountPaise());
            transfersDeclined.increment();
            return transfer;
        }
        if (wallets.credit(transferRequest.to(), transferRequest.amountPaise()) != 1)
            throw new IllegalStateException("Destination wallet disappeared");
        transfer.complete();
        log.info("event=transfer_completed transferId={} from={} to={} amountPaise={}", transfer.getId(),
                transferRequest.from(), transferRequest.to(), transferRequest.amountPaise());
        return transfer;
    }

    @Transactional(readOnly = true)
    public Transfer get(Long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFound("Transfer not found: " + id));
    }

    private static void validate(TransferRequest transferRequest, String key) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Idempotency-Key header is required");
        if (transferRequest.from().equals(transferRequest.to()))
            throw new IllegalArgumentException("from and to wallets must differ");
        if (transferRequest.amountPaise() <= 0)
            throw new IllegalArgumentException("amountPaise must be positive");
    }

    private static String hash(TransferRequest transferRequest) {
        try {
            String rawInput = transferRequest.from() + "|" + transferRequest.to() + "|" + transferRequest.amountPaise();
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(rawInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String m) {
            super(m);
        }
    }

    public static class IdempotencyConflict extends RuntimeException {
        public IdempotencyConflict(String m) {
            super(m);
        }
    }
}
