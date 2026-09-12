package com.paytm.wallet.service;

import com.paytm.wallet.domain.Wallet;
import com.paytm.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletService {
    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository repo;

    public WalletService(WalletRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Wallet getOrCreate(String userId) {
        int inserted = repo.insertIfAbsent(userId);
        Wallet w = repo.findByUserId(userId).orElseThrow(() -> new IllegalStateException("Wallet disappeared after get-or-create"));
        if (inserted == 1) {
            log.info("event=wallet_created walletId={} userId={}", w.getId(), userId);
        }
        return w;
    }

    @Transactional(readOnly = true)
    public Wallet get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Wallet not found: " + id));
    }

    @Transactional
    public Wallet fundForTest(Long id, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amountPaise must be positive");
        if (repo.credit(id, amount) != 1) throw new NotFound("Wallet not found: " + id);
        return repo.findById(id).orElseThrow();
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String m) {
            super(m);
        }
    }
}
