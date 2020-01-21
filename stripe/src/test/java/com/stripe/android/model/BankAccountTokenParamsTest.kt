package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BankAccountTokenParamsTest {
    @Test
    fun toParamMap_createsExpectedMap() {
        val bankAccountTokenParams = BankAccountTokenParams(
            accountNumber = BANK_ACCOUNT_NUMBER,
            country = "US",
            currency = "usd",
            routingNumber = BANK_ROUTING_NUMBER,
            accountHolderType = BankAccountTokenParams.Type.Individual,
            accountHolderName = BANK_ACCOUNT_HOLDER_NAME
        )
        val bankAccountMap =
            bankAccountTokenParams.toParamMap()[Token.TokenType.BANK_ACCOUNT] as Map<String, String>
        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap["account_number"])
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap["routing_number"])
        assertEquals("US", bankAccountMap["country"])
        assertEquals("usd", bankAccountMap["currency"])
        assertEquals(BANK_ACCOUNT_HOLDER_NAME, bankAccountMap["account_holder_name"])
        assertEquals(BankAccountTokenParams.Type.Individual.code, bankAccountMap["account_holder_type"])
    }

    private companion object {
        private const val BANK_ACCOUNT_NUMBER = "000123456789"
        private const val BANK_ROUTING_NUMBER = "110000000"
        private const val BANK_ACCOUNT_HOLDER_NAME = "Lily Thomas"
    }
}
