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
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet getOrCreate(String userId) {
        int inserted = walletRepository.insertIfAbsent(userId);
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(() -> new IllegalStateException("Wallet disappeared after get-or-create"));
        if (inserted == 1) {
            log.info("event=wallet_created walletId={} userId={}", wallet.getId(), userId);
        }
        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet get(Long id) {
        return walletRepository.findById(id).orElseThrow(() -> new NotFound("Wallet not found: " + id));
    }

    @Transactional
    public Wallet fundForTest(Long id, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amountPaise must be positive");
        if (walletRepository.credit(id, amount) != 1) throw new NotFound("Wallet not found: " + id);
        return walletRepository.findById(id).orElseThrow();
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String m) {
            super(m);
        }
    }
}
