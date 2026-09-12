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

    public TransferService(WalletRepository w, TransferRepository t, MeterRegistry registry) {
        wallets = w;
        transfers = t;
        transfersCreated = registry.counter("transfers_created_total");
        transfersDeclined = registry.counter("transfers_declined_insufficient_funds_total");
        idempotentReplays = registry.counter("transfers_idempotent_replays_total");
    }

    @Transactional
    public Transfer transfer(TransferRequest r, String key) {
        validate(r, key);
        String hash = hash(r);
        int inserted = transfers.reserve(r.from(), r.to(), r.amountPaise(), key, hash);
        if (inserted == 0) {
            Transfer existing = transfers.findByIdempotencyKey(key).orElseThrow();
            if (!existing.getRequestHash().equals(hash))
                throw new IdempotencyConflict("Idempotency key reused with a different request");
            log.info("event=idempotent_replay transferId={} idempotencyKey={}", existing.getId(), key);
            idempotentReplays.increment();
            return existing;
        }
        long first = Math.min(r.from(), r.to()), second = Math.max(r.from(), r.to());
        wallets.findByIdForUpdate(first).orElseThrow(() -> new WalletService.NotFound("Wallet not found: " + first));
        wallets.findByIdForUpdate(second).orElseThrow(() -> new WalletService.NotFound("Wallet not found: " + second));
        int debited = wallets.debitIfSufficient(r.from(), r.amountPaise());
        Transfer t = transfers.findByIdempotencyKey(key).orElseThrow();
        
        log.info("event=transfer_created transferId={} from={} to={} amountPaise={}", t.getId(), r.from(), r.to(), r.amountPaise());
        transfersCreated.increment();
        
        if (debited == 0) {
            t.decline();
            log.info("event=transfer_declined_insufficient_funds transferId={} from={} to={} amountPaise={}", t.getId(), r.from(), r.to(), r.amountPaise());
            transfersDeclined.increment();
            return t;
        }
        if (wallets.credit(r.to(), r.amountPaise()) != 1)
            throw new IllegalStateException("Destination wallet disappeared");
        t.complete();
        log.info("event=transfer_completed transferId={} from={} to={} amountPaise={}", t.getId(), r.from(), r.to(), r.amountPaise());
        return t;
    }

    @Transactional(readOnly = true)
    public Transfer get(Long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFound("Transfer not found: " + id));
    }

    private static void validate(TransferRequest r, String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
        if (r.from().equals(r.to())) throw new IllegalArgumentException("from and to wallets must differ");
        if (r.amountPaise() <= 0) throw new IllegalArgumentException("amountPaise must be positive");
    }

    private static String hash(TransferRequest r) {
        try {
            String s = r.from() + "|" + r.to() + "|" + r.amountPaise();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
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
