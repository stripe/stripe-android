package com.stripe.android.model

object BankAccountTokenParamsFixtures {
    @JvmField
    val DEFAULT: BankAccountTokenParams = BankAccountTokenParams(
        country = "US",
        currency = "usd",
        accountNumber = "000123456789",
        routingNumber = "110000000",
        accountHolderName = "Jenny Rosen",
        accountHolderType = BankAccountTokenParams.Type.Individual
    )
}
