package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentMethodCreateParamsTest {

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
                "exp_year" to 2054,
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
    fun createWithOverriddenParamMap_toParamMap_shouldCreateExpectedMap() {
        val map = mapOf(
            "type" to "test",
            "bacs_debit" to mapOf(
                "account_number" to "00012345",
                "sort_code" to "108800"
            ),
            "some_key" to mapOf(
                "other_key" to mapOf(
                    "third_key" to "value"
                )
            ),
            "phone" to "1-800-555-1234"
        )

        assertThat(
            PaymentMethodCreateParams(
                PaymentMethod.Type.Card,
                overrideParamMap = map,
                productUsage = emptySet()
            ).toParamMap()
        ).isEqualTo(map)
    }

    @Test
    fun equals_withFpx() {
        assertThat(createFpx())
            .isEqualTo(createFpx())
    }

    @Test
    fun attribution_whenFpxAndProductUsageIsEmpty_shouldBeNull() {
        val params = createFpx()
        assertThat(params.attribution)
            .isEmpty()
    }

    @Test
    fun attribution_whenFpxAndProductUsageIsNotEmpty_shouldBeProductUsage() {
        val expectedProductUsage = "example_product_usage"
        val params = createFpx().copy(
            productUsage = setOf(expectedProductUsage)
        )
        assertEquals(
            setOf(expectedProductUsage),
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
        val expectedProductUsage = "example_product_usage"
        val params = PaymentMethodCreateParams.create(
            PaymentMethodCreateParamsFixtures.CARD_WITH_ATTRIBUTION
        ).copy(
            productUsage = setOf(expectedProductUsage)
        )
        assertThat(params.attribution)
            .containsExactly(
                "CardMultilineWidget",
                expectedProductUsage
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
                type = PaymentMethod.Type.Card,
                card = PaymentMethodCreateParams.Card(
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    expiryMonth = 12,
                    expiryYear = 2045,
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

    @Test
    fun `createLink correctly set parameters`() {
        val paymentDetailsId = "payment_details_123"
        val consumerSessionClientSecret = "client_secret_123"
        val extraParams = mapOf(
            "card" to mapOf(
                "cvc" to "123"
            )
        )

        assertThat(
            PaymentMethodCreateParams.createLink(
                paymentDetailsId,
                consumerSessionClientSecret,
                extraParams
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "link",
                "link" to mapOf(
                    "payment_details_id" to paymentDetailsId,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "card" to mapOf(
                        "cvc" to "123"
                    )
                )
            )
        )
    }

    @Test
    fun `createBacsFromParams should return a 'BacsDebit' instance correctly`() {
        val params = PaymentMethodCreateParams.create(
            bacsDebit = PaymentMethodCreateParams.BacsDebit(
                accountNumber = "00012345",
                sortCode = "108800"
            ),
            billingDetails = PaymentMethod.BillingDetails()
        )

        assertThat(
            PaymentMethodCreateParams.createBacsFromParams(params)
        ).isEqualTo(
            PaymentMethodCreateParams.BacsDebit(
                accountNumber = "00012345",
                sortCode = "108800"
            )
        )

        val overrideParams = PaymentMethodCreateParams.createWithOverride(
            code = "bacs_debit",
            overrideParamMap = mapOf(
                "bacs_debit" to mapOf(
                    "account_number" to "00012345",
                    "sort_code" to "108800"
                )
            ),
            productUsage = setOf(),
            billingDetails = null,
            requiresMandate = false
        )

        assertThat(
            PaymentMethodCreateParams.createBacsFromParams(overrideParams)
        ).isEqualTo(
            PaymentMethodCreateParams.BacsDebit(
                accountNumber = "00012345",
                sortCode = "108800"
            )
        )
    }

    @Test
    fun `getNameFromOverrideParams should return name from billing details if available`() {
        val params = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card(),
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe"
            )
        )

        assertThat(
            PaymentMethodCreateParams.getNameFromParams(params)
        ).isEqualTo("John Doe")

        val overrideParams = PaymentMethodCreateParams.createWithOverride(
            code = "bacs_debit",
            overrideParamMap = mapOf(
                "billing_details" to mapOf(
                    "name" to "John Doe"
                )
            ),
            productUsage = setOf(),
            billingDetails = null,
            requiresMandate = false
        )

        assertThat(
            PaymentMethodCreateParams.getNameFromParams(overrideParams)
        ).isEqualTo("John Doe")
    }

    @Test
    fun `getEmailFromOverrideParams should return email from billing details if available`() {
        val params = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card(),
            billingDetails = PaymentMethod.BillingDetails(
                email = "johndoe@email.com"
            )
        )

        assertThat(
            PaymentMethodCreateParams.getEmailFromParams(params)
        ).isEqualTo("johndoe@email.com")

        val overrideParams = PaymentMethodCreateParams.createWithOverride(
            code = "card",
            overrideParamMap = mapOf(
                "billing_details" to mapOf(
                    "email" to "johndoe@email.com"
                )
            ),
            productUsage = setOf(),
            billingDetails = null,
            requiresMandate = false
        )

        assertThat(
            PaymentMethodCreateParams.getEmailFromParams(overrideParams)
        ).isEqualTo("johndoe@email.com")
    }

    @Test
    fun `create() with 'allow_redisplay' set for card returns expected values`() {
        val card = PaymentMethodCreateParams.Card(
            number = CardNumberFixtures.VISA_NO_SPACES,
            expiryMonth = 12,
            expiryYear = 2045,
            cvc = "123",
        )

        assertThat(
            PaymentMethodCreateParams.create(
                card = card,
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "unspecified")

        assertThat(
            PaymentMethodCreateParams.create(
                card = card,
                allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "limited")

        assertThat(
            PaymentMethodCreateParams.create(
                card = card,
                allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
            ).toParamMap()
        ).containsEntry("allow_redisplay", "always")
    }

    @Test
    fun `create() with 'allow_redisplay' set for US Bank Account returns expected values`() {
        assertThat(
            PaymentMethodCreateParams.createUSBankAccount(
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "unspecified")

        assertThat(
            PaymentMethodCreateParams.createUSBankAccount(
                allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "limited")

        assertThat(
            PaymentMethodCreateParams.createUSBankAccount(
                allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
            ).toParamMap()
        ).containsEntry("allow_redisplay", "always")
    }

    @Test
    fun `create() with 'allow_redisplay' set for SEPA Debit returns expected values`() {
        assertThat(
            PaymentMethodCreateParams.create(
                sepaDebit = PaymentMethodCreateParams.SepaDebit(
                    iban = "12345678901234556"
                ),
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "unspecified")

        assertThat(
            PaymentMethodCreateParams.create(
                sepaDebit = PaymentMethodCreateParams.SepaDebit(
                    iban = "12345678901234556"
                ),
                allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED
            ).toParamMap()
        ).containsEntry("allow_redisplay", "limited")

        assertThat(
            PaymentMethodCreateParams.create(
                sepaDebit = PaymentMethodCreateParams.SepaDebit(
                    iban = "12345678901234556"
                ),
                allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
            ).toParamMap()
        ).containsEntry("allow_redisplay", "always")
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
