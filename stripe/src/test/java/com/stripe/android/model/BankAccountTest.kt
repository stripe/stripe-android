package com.stripe.android.model

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [BankAccount].
 */
class BankAccountTest {

    @Test
    fun parseSampleAccount_returnsExpectedValue() {
        val expectedAccount = BankAccount(
            "Jane Austen",
            BankAccount.BankAccountType.INDIVIDUAL,
            "STRIPE TEST BANK",
            "US",
            "usd",
            "1JWtPxqbdX5Gamtc",
            "6789",
            "110000000"
        )
        assertEquals(expectedAccount, BankAccountFixtures.BANK_ACCOUNT)
    }

    @Test
    fun createBankTokenParams_hasExpectedEntries() {
        val bankAccount = BankAccount(BANK_ACCOUNT_NUMBER, "US",
            "usd", BANK_ROUTING_NUMBER)
        val bankAccountMap = getBankAccountTokenParamData(bankAccount)
        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap["account_number"])
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap["routing_number"])
        assertEquals("US", bankAccountMap["country"])
        assertEquals("usd", bankAccountMap["currency"])
    }

    @Test
    fun hashMapFromBankAccount_mapsCorrectFields() {
        val bankAccount = BankAccount(BANK_ACCOUNT_NUMBER,
            BANK_ACCOUNT_HOLDER_NAME, BankAccount.BankAccountType.INDIVIDUAL, null, "US",
            "usd", null, null, BANK_ROUTING_NUMBER)
        val bankAccountMap = getBankAccountTokenParamData(bankAccount)
        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap["account_number"])
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap["routing_number"])
        assertEquals("US", bankAccountMap["country"])
        assertEquals("usd", bankAccountMap["currency"])
        assertEquals(BANK_ACCOUNT_HOLDER_NAME, bankAccountMap["account_holder_name"])
        assertEquals(BankAccount.BankAccountType.INDIVIDUAL, bankAccountMap["account_holder_type"])
    }

    private fun getBankAccountTokenParamData(bankAccount: BankAccount): Map<String, Any> {
        val params = bankAccount.toParamMap().plus(GUID_PARAMS)
        return params["bank_account"] as Map<String, Any>
    }

    companion object {
        private const val BANK_ACCOUNT_NUMBER = "000123456789"
        private const val BANK_ROUTING_NUMBER = "110000000"
        private const val BANK_ACCOUNT_HOLDER_NAME = "Lily Thomas"

        private val GUID_PARAMS = mapOf(
            "guid" to UUID.randomUUID().toString(),
            "muid" to UUID.randomUUID().toString()
        )
    }
}
