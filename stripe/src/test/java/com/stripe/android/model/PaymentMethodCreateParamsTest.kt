package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMethodCreateParamsTest {

    @Test
    fun card_toPaymentMethodParamsCard() {
        val expectedCard = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            cvc = "123",
            expiryMonth = 8,
            expiryYear = 2019
        )
        assertEquals(expectedCard, CardFixtures.CARD.toPaymentMethodParamsCard())
    }

    @Test
    fun createFromGooglePay_withNoBillingAddress() {
        val createdParams = PaymentMethodCreateParams.createFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.create("tok_1F4ACMCRMbs6FrXf6fPqLnN7"),
            PaymentMethod.BillingDetails.Builder()
                .build()
        )
        assertEquals(expectedParams, createdParams)
    }

    @Test
    fun createFromGooglePay_withFullBillingAddress() {
        val createdParams = PaymentMethodCreateParams.createFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.create("tok_1F4VSjBbvEcIpqUbSsbEtBap"),
            PaymentMethod.BillingDetails(
                phone = "1-888-555-1234",
                email = "stripe@example.com",
                name = "Stripe Johnson",
                address = Address(
                    line1 = "510 Townsend St",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94103",
                    country = "US"
                )
            )
        )
        assertEquals(expectedParams, createdParams)
    }

    @Test
    fun createCardParams() {
        val expectedParams = mapOf(
            "number" to "4242424242424242",
            "exp_month" to 1,
            "exp_year" to 2024,
            "cvc" to "111"
        )
        assertEquals(
            expectedParams,
            PaymentMethodCreateParamsFixtures.CARD.toParamMap()
        )
    }

    @Test
    fun createSepaDebit() {
        val expectedParams = mapOf(
            "type" to "sepa_debit",
            "sepa_debit" to mapOf("iban" to "my_iban")
        )
        assertEquals(
            expectedParams,
            PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT.toParamMap()
        )
    }

    @Test
    fun auBecsDebit_toParamMap_shouldCreateExpectedMap() {
        assertThat(PaymentMethodCreateParamsFixtures.AU_BECS_DEBIT.toParamMap())
            .isEqualTo(
                mapOf(
                    "type" to "au_becs_debit",
                    "au_becs_debit" to mapOf(
                        "bsb_number" to "000000",
                        "account_number" to "000123456"
                    ),
                    "billing_details" to mapOf(
                        "address" to mapOf(
                            "city" to "Los Angeles",
                            "country" to "US",
                            "line1" to "123 Main St",
                            "state" to "CA"
                        ),
                        "email" to "me@example.com",
                        "name" to "Home",
                        "phone" to "1-800-555-1234"
                    )
                )
            )
    }

    @Test
    fun equals_withFpx() {
        assertEquals(createFpx(), createFpx())
    }

    private fun createFpx(): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Fpx(bank = "hsbc"),
            PaymentMethod.BillingDetails(
                phone = "1-888-555-1234",
                email = "stripe@example.com",
                name = "Stripe Johnson",
                address = Address(
                    line1 = "510 Townsend St",
                    line2 = "",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94103",
                    country = "US"
                )
            )
        )
    }
}
