package com.paytm.wallet.repository;

import com.paytm.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id=:id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = "INSERT INTO wallets(user_id,balance_paise) VALUES(:userId,0) ON CONFLICT(user_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE wallets SET balance_paise=balance_paise-:amount WHERE id=:walletId AND balance_paise>=:amount", nativeQuery = true)
    int debitIfSufficient(@Param("walletId") Long walletId, @Param("amount") long amount);

    @Modifying
    @Query(value = "UPDATE wallets SET balance_paise=balance_paise+:amount WHERE id=:walletId", nativeQuery = true)
    int credit(@Param("walletId") Long walletId, @Param("amount") long amount);
}
