package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.view.AddPaymentMethodActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentMethodCreateParamsTest {

    @Test
    fun card_toPaymentMethodParamsCard() {
        assertThat(CardFixtures.CARD.toPaymentMethodParamsCard()).isEqualTo(
            PaymentMethodCreateParams.Card(
                number = "4242424242424242",
                cvc = "123",
                expiryMonth = 8,
                expiryYear = 2050
            )
        )
    }

    @Test
    fun createFromGooglePay_withNoBillingAddress() {
        assertThat(
            PaymentMethodCreateParams.createFromGooglePay(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS
            )
        ).isEqualTo(
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card(
                    token = "tok_1F4ACMCRMbs6FrXf6fPqLnN7",
                    attribution = setOf("GooglePay")
                ),
                PaymentMethod.BillingDetails.Builder()
                    .build()
            )
        )
    }

    @Test
    fun createFromGooglePay_withFullBillingAddress() {
        assertThat(
            PaymentMethodCreateParams.createFromGooglePay(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS
            )
        ).isEqualTo(
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card(
                    token = "tok_1F4VSjBbvEcIpqUbSsbEtBap",
                    attribution = setOf("GooglePay")
                ),
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
        )
    }

    @Test
    fun createCardParams() {
        assertThat(
            PaymentMethodCreateParamsFixtures.CARD.toParamMap()
        ).isEqualTo(
            mapOf(
                "number" to "4242424242424242",
                "exp_month" to 1,
                "exp_year" to 2024,
                "cvc" to "111"
            )
        )
    }

    @Test
    fun createSepaDebit() {
        assertThat(PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT.toParamMap())
            .isEqualTo(
                mapOf(
                    "type" to "sepa_debit",
                    "sepa_debit" to mapOf("iban" to "my_iban")
                )
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
                            "city" to "San Francisco",
                            "country" to "US",
                            "line1" to "1234 Main St",
                            "state" to "CA",
                            "postal_code" to "94111"
                        ),
                        "email" to "jenny.rosen@example.com",
                        "name" to "Jenny Rosen",
                        "phone" to "1-800-555-1234"
                    )
                )
            )
    }

    @Test
    fun bacsDebit_toParamMap_shouldCreateExpectedMap() {
        assertThat(PaymentMethodCreateParamsFixtures.BACS_DEBIT.toParamMap())
            .isEqualTo(
                mapOf(
                    "type" to "bacs_debit",
                    "bacs_debit" to mapOf(
                        "account_number" to "00012345",
                        "sort_code" to "108800"
                    ),
                    "billing_details" to mapOf(
                        "address" to mapOf(
                            "city" to "San Francisco",
                            "country" to "US",
                            "line1" to "1234 Main St",
                            "state" to "CA",
                            "postal_code" to "94111"
                        ),
                        "email" to "jenny.rosen@example.com",
                        "name" to "Jenny Rosen",
                        "phone" to "1-800-555-1234"
                    )
                )
            )
    }

    @Test
    fun equals_withFpx() {
        assertEquals(createFpx(), createFpx())
    }

    @Test
    fun attribution_whenFpxAndProductUsageIsEmpty_shouldBeNull() {
        val params = createFpx()
        assertNull(params.attribution)
    }

    @Test
    fun attribution_whenFpxAndProductUsageIsNotEmpty_shouldBeProductUsage() {
        val params = createFpx().copy(
            productUsage = setOf(AddPaymentMethodActivity.PRODUCT_TOKEN)
        )
        assertEquals(
            setOf(AddPaymentMethodActivity.PRODUCT_TOKEN),
            params.attribution
        )
    }

    @Test
    fun attribution_whenCardAndProductUsageIsEmpty_shouldBeAttribution() {
        val params = PaymentMethodCreateParams.create(
            PaymentMethodCreateParamsFixtures.CARD_WITH_ATTRIBUTION
        )
        assertEquals(
            setOf("CardMultilineWidget"),
            params.attribution
        )
    }

    @Test
    fun attribution_whenCardAndProductUsageIsNotEmpty_shouldBeAttributionPlusProductUsage() {
        val params = PaymentMethodCreateParams.create(
            PaymentMethodCreateParamsFixtures.CARD_WITH_ATTRIBUTION
        ).copy(
            productUsage = setOf(AddPaymentMethodActivity.PRODUCT_TOKEN)
        )
        assertThat(params.attribution)
            .containsExactly(
                "CardMultilineWidget",
                AddPaymentMethodActivity.PRODUCT_TOKEN
            )
    }

    @Test
    fun `createCard() with CardParams returns expected PaymentMethodCreateParams`() {
        val cardParams = CardParamsFixtures.DEFAULT
            .copy(loggingTokens = setOf("CardInputView"))

        assertThat(
            PaymentMethodCreateParams.createCard(cardParams)
        ).isEqualTo(
            PaymentMethodCreateParams(
                type = PaymentMethodCreateParams.Type.Card,
                card = PaymentMethodCreateParams.Card(
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    expiryMonth = 12,
                    expiryYear = 2025,
                    cvc = "123",
                    attribution = setOf("CardInputView")
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = cardParams.name,
                    address = cardParams.address
                ),
                metadata = mapOf("fruit" to "orange")
            )
        )
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
