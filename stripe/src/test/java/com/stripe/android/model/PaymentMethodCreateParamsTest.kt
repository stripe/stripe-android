package com.stripe.android.model

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
