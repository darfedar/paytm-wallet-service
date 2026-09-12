package com.paytm.wallet.repository;

import com.paytm.wallet.domain.Transfer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String key);

    @Modifying
    @Query(value = "INSERT INTO transfers(from_wallet_id,to_wallet_id,amount_paise,idempotency_key,request_hash,status) VALUES(:fromId,:toId,:amount,:key,:hash,'DECLINED_INSUFFICIENT_FUNDS') ON CONFLICT(idempotency_key) DO NOTHING", nativeQuery = true)
    int reserve(@Param("fromId") Long fromId, @Param("toId") Long toId, @Param("amount") long amount, @Param("key") String key, @Param("hash") String hash);
}
