# Wallet & P2P Transfer Service

This repository contains a simple, highly-concurrent peer-to-peer wallet and transfer service built with Spring Boot, PostgreSQL, and Flyway. It is designed to prioritize correctness (conservation of money and absolute prevention of overdrafts/double-spends) under heavy concurrency.

## Data Model

The application uses a minimalist, fully-relational schema with strict database constraints:
- `wallets`: Stores the user identifier and `balance_paise`. Guaranteed safe via a `CHECK (balance_paise >= 0)` constraint and a `UNIQUE (user_id)` constraint.
- `transfers`: An immutable ledger of money movements. Includes a unique `idempotency_key` constraint to enforce exactly-once semantics natively at the database level.

Money is strictly stored and transferred as integer `paise` (`BIGINT` in PostgreSQL). Floats/decimals are strictly avoided to prevent rounding errors and destroyed money.

## Concurrency & Safety (Design Decisions)

### 1. Simplest-Correct Mechanism for Conservation
To ensure money is conserved and overdrafts are impossible, the service uses an **atomic conditional update** combined with a **deterministic sorted lock**:

```sql
UPDATE wallets SET balance_paise = balance_paise - :amount WHERE id = :walletId AND balance_paise >= :amount
```
**Why this approach?**
By using `UPDATE ... WHERE balance >= amount`, the database itself atomically checks the condition during the update. If the balance drops below the threshold, the update affects 0 rows, and the application cleanly declines the transfer without partial application. 

**Deadlock Avoidance:**
Before the debit/credit sequence, the application executes `SELECT ... FOR UPDATE` locks on both wallets. To avoid deadlocks when `A -> B` and `B -> A` transfers happen simultaneously, the locks are **deterministically ordered** (lowest wallet ID is always locked first). 

**Rejected Alternatives:**
- *Serializable Isolation Level*: Rejected because it relies on optimistic concurrency control. Under heavy P2P contention (e.g. a "burst" of requests between the same users), `SERIALIZABLE` causes massive serialization failures and requires the application to aggressively retry, burning CPU and DB connections.
- *App-level Math*: Reading the balance into memory (`val = val - amount`) and saving it back is a textbook race condition (Lost Update anomaly). It was completely avoided.

### 2. Where Idempotency Lives
Idempotency is enforced strictly at the database level using a `UNIQUE` constraint on `transfers.idempotency_key`.

**Crucially, the idempotency key is inserted in the exact same transaction as the ledger movement.** 
If a concurrent duplicate request with the same key arrives, it loses the insert race (`ON CONFLICT` / `DataIntegrityViolationException`), and safely reads the committed result of the first request.

**Rejected Alternative (TOCTOU flaw):** Checking if the key exists in a *separate* transaction before doing the transfer introduces a Time-Of-Check-To-Time-Of-Use race condition, which leads to a double debit under a retry storm.
A same-key with a *different* body is detected via a SHA-256 hash comparison of the request payload, safely rejecting it with a `409 Conflict`.

### 3. Consistency vs Availability
Given this is a financial workload, the service heavily prioritizes **Consistency (C) over Availability (A)** in the CAP theorem. 
- A wallet get-or-create will briefly block concurrent identical requests.
- P2P transfers are pessimistic. If the database goes down, or if lock contention spikes, the system will degrade availability (timeout/fail) rather than risk creating a split-brain ledger where money is destroyed or double-spent.

## Observability

- **Structured Logging:** Emits JSON logs equipped with an `X-Correlation-Id` tracking the complete lifecycle of a request (`wallet_created`, `transfer_created`, `transfer_completed`, `transfer_declined_insufficient_funds`).
- **Prometheus Metrics:** Configured via `micrometer-registry-prometheus` to expose latency p99 histograms, throughput, and custom domain counters:
  - `transfers_created_total`
  - `transfers_declined_insufficient_funds_total`
  - `transfers_idempotent_replays_total`

## Disclosure
- AI (LLMs) were used strictly in a *directed* capacity. Architectural decisions (sorted locking, conditional updates, idempotency primitives) were manually chosen and validated, while AI was directed to assist with syntax formatting and scaffolding boilerplate.
- **Cost:** This stack is built strictly with open-source tools and is deployable on entirely free tiers (e.g., Render Web Services + Render PostgreSQL) for a total cost of **₹0**.
