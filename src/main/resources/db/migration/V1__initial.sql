CREATE TABLE wallets (
 id BIGSERIAL PRIMARY KEY,
 user_id VARCHAR(100) NOT NULL,
 balance_paise BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_wallet_user UNIQUE(user_id),
 CONSTRAINT chk_wallet_balance_non_negative CHECK(balance_paise >= 0)
);
CREATE TABLE transfers (
 id BIGSERIAL PRIMARY KEY,
 from_wallet_id BIGINT NOT NULL REFERENCES wallets(id),
 to_wallet_id BIGINT NOT NULL REFERENCES wallets(id),
 amount_paise BIGINT NOT NULL,
 idempotency_key VARCHAR(200) NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 status VARCHAR(40) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 completed_at TIMESTAMPTZ,
 CONSTRAINT uq_transfer_idempotency_key UNIQUE(idempotency_key),
 CONSTRAINT chk_transfer_amount_positive CHECK(amount_paise > 0),
 CONSTRAINT chk_transfer_different_wallets CHECK(from_wallet_id <> to_wallet_id)
);
CREATE INDEX idx_transfers_from ON transfers(from_wallet_id);
CREATE INDEX idx_transfers_to ON transfers(to_wallet_id);
