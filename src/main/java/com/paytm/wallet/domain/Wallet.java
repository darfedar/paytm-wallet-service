package com.paytm.wallet.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(name = "uq_wallet_user", columnNames = "user_id"))
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "balance_paise", nullable = false)
    private long balancePaise;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;


    protected Wallet() {
    }

    public Wallet(String userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public long getBalancePaise() {
        return balancePaise;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
