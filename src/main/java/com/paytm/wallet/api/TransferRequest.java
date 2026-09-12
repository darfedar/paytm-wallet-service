package com.paytm.wallet.api;

import jakarta.validation.constraints.*;

public record TransferRequest(
        @NotNull @Positive Long from,
        @NotNull @Positive Long to,
        @Positive long amountPaise
) { }
