package com.paytm.wallet.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transfers", uniqueConstraints = @UniqueConstraint(name = "uq_transfer_idempotency_key", columnNames = "idempotency_key"))
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_wallet_id", nullable = false)
    private Long fromWalletId;


    @Column(name = "to_wallet_id", nullable = false)
    private Long toWalletId;
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transfer() {
    }

    public Transfer(Long from, Long to, long amount, String key, String hash) {
        fromWalletId = from;
        toWalletId = to;
        amountPaise = amount;
        idempotencyKey = key;
        requestHash = hash;
        status = TransferStatus.DECLINED_INSUFFICIENT_FUNDS;
    }

    public void complete() {
        status = TransferStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void decline() {
        status = TransferStatus.DECLINED_INSUFFICIENT_FUNDS;
        completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getFromWalletId() {
        return fromWalletId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
