package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class BankAccountTokenParamsTest {
    @Test
    fun toParamMap_createsExpectedMap() {
        val params = BankAccountTokenParams(
            accountNumber = BANK_ACCOUNT_NUMBER,
            country = "US",
            currency = "usd",
            routingNumber = BANK_ROUTING_NUMBER,
            accountHolderType = BankAccountTokenParams.Type.Individual,
            accountHolderName = BANK_ACCOUNT_HOLDER_NAME
        ).toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "bank_account" to mapOf(
                        "account_number" to BANK_ACCOUNT_NUMBER,
                        "routing_number" to BANK_ROUTING_NUMBER,
                        "country" to "US",
                        "currency" to "usd",
                        "account_holder_name" to BANK_ACCOUNT_HOLDER_NAME,
                        "account_holder_type" to BankAccountTokenParams.Type.Individual.code
                    )
                )
            )
    }

    private companion object {
        private const val BANK_ACCOUNT_NUMBER = "000123456789"
        private const val BANK_ROUTING_NUMBER = "110000000"
        private const val BANK_ACCOUNT_HOLDER_NAME = "Lily Thomas"
    }
}
