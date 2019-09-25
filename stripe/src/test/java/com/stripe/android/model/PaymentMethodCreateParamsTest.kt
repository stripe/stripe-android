package com.stripe.android.model

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMethodCreateParamsTest {

    @Test
    fun card_toPaymentMethodParamsCard() {
        val expectedCard = PaymentMethodCreateParams.Card.Builder()
            .setNumber("4242424242424242")
            .setCvc("123")
            .setExpiryMonth(8)
            .setExpiryYear(2019)
            .build()
        assertEquals(expectedCard, CardFixtures.CARD.toPaymentMethodParamsCard())
    }

    @Test
    @Throws(JSONException::class)
    fun createFromGooglePay_withNoBillingAddress() {
        val createdParams = PaymentMethodCreateParams.createFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.create("tok_1F4ACMCRMbs6FrXf6fPqLnN7"),
            PaymentMethod.BillingDetails.Builder()
                .setEmail("")
                .build()
        )
        assertEquals(expectedParams, createdParams)
    }

    @Test
    @Throws(JSONException::class)
    fun createFromGooglePay_withFullBillingAddress() {
        val createdParams = PaymentMethodCreateParams.createFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS)

        val expectedParams = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.create("tok_1F4VSjBbvEcIpqUbSsbEtBap"),
            PaymentMethod.BillingDetails.Builder()
                .setPhone("1-888-555-1234")
                .setEmail("stripe@example.com")
                .setName("Stripe Johnson")
                .setAddress(Address.Builder()
                    .setLine1("510 Townsend St")
                    .setLine2("")
                    .setCity("San Francisco")
                    .setState("CA")
                    .setPostalCode("94103")
                    .setCountry("US")
                    .build())
                .build()
        )
        assertEquals(expectedParams, createdParams)
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
            PaymentMethodCreateParams.Fpx.Builder()
                .setBank("hsbc")
                .build(),
            PaymentMethod.BillingDetails.Builder()
                .setPhone("1-888-555-1234")
                .setEmail("stripe@example.com")
                .setName("Stripe Johnson")
                .setAddress(Address.Builder()
                    .setLine1("510 Townsend St")
                    .setLine2("")
                    .setCity("San Francisco")
                    .setState("CA")
                    .setPostalCode("94103")
                    .setCountry("US")
                    .build())
                .build()
        )
    }
}
