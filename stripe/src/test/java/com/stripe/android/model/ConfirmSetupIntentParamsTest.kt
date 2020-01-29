package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfirmSetupIntentParamsTest {

    @Test
    fun shouldUseStripeSdk_withPaymentMethodId() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            "pm_123", "client_secret", "return_url")
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk())

        assertTrue(confirmSetupIntentParams
            .withShouldUseStripeSdk(true)
            .shouldUseStripeSdk())
    }

    @Test
    fun shouldUseStripeSdk_withPaymentMethodCreateParams() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            "client_secret",
            "return_url"
        )
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk())

        assertTrue(confirmSetupIntentParams
            .withShouldUseStripeSdk(true)
            .shouldUseStripeSdk())
    }

    @Test
    fun create_withPaymentMethodId_shouldPopulateParamMapCorrectly() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            "pm_12345",
            "client_secret",
            null
        )
        val params = confirmSetupIntentParams.toParamMap()
        assertNull(params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA])
        assertEquals("pm_12345",
            params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_ID])
    }

    @Test
    fun create_withPaymentMethodCreateParams_shouldPopulateParamMapCorrectly() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            "client_secret", null
        )
        val params = confirmSetupIntentParams.toParamMap()
        assertNull(params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_ID])
        val paymentMethodData =
            params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA] as Map<String, Any>
        assertEquals("card", paymentMethodData["type"])
        assertNotNull(paymentMethodData["card"])
    }

    @Test
    fun create_withoutPaymentMethod() {
        val params = ConfirmSetupIntentParams.createWithoutPaymentMethod("client_secret")
        assertNull(params.paymentMethodCreateParams)
        assertNull(params.paymentMethodId)
    }

    @Test
    fun create_withSepaDebitPaymentMethodParams_shouldUseDefaultMandateDataIfNotSpecified() {
        val expectedParams = mapOf(
            "client_secret" to CLIENT_SECRET,
            "use_stripe_sdk" to false,
            "mandate_data" to mapOf(
                "customer_acceptance" to mapOf(
                    "type" to "online",
                    "online" to mapOf(
                        "infer_from_client" to true
                    )
                )
            ),
            "payment_method_data" to mapOf(
                "type" to "sepa_debit",
                "sepa_debit" to mapOf(
                    "iban" to "my_iban"
                )
            )
        )

        val actualParams = ConfirmSetupIntentParams.create(
            clientSecret = CLIENT_SECRET,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT
        ).toParamMap()
        assertEquals(expectedParams, actualParams)
    }

    @Test
    fun create_withSepaDebitPaymentMethodParams_shouldUseMandateDataIfSpecified() {
        val expectedParams = mapOf(
            "client_secret" to CLIENT_SECRET,
            "use_stripe_sdk" to false,
            "mandate_data" to mapOf(
                "customer_acceptance" to mapOf(
                    "type" to "online",
                    "online" to mapOf(
                        "ip_address" to "127.0.0.1",
                        "user_agent" to "my_user_agent"
                    )
                )
            ),
            "payment_method_data" to mapOf(
                "type" to "sepa_debit",
                "sepa_debit" to mapOf(
                    "iban" to "my_iban"
                )
            )
        )

        val actualParams = ConfirmSetupIntentParams.create(
            clientSecret = CLIENT_SECRET,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
            mandateData = MandateDataParamsFixtures.DEFAULT
        ).toParamMap()
        assertEquals(expectedParams, actualParams)
    }

    @Test
    fun create_withSepaDebitPaymentMethodParams_shouldUseMandateIdIfSpecified() {
        val expectedParams = mapOf(
            "client_secret" to CLIENT_SECRET,
            "use_stripe_sdk" to false,
            "mandate" to "mandate_123456789",
            "payment_method_data" to mapOf(
                "type" to "sepa_debit",
                "sepa_debit" to mapOf(
                    "iban" to "my_iban"
                )
            )
        )
        val actualParams =
            ConfirmSetupIntentParams.create(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
                mandateId = "mandate_123456789"
            ).toParamMap()
        assertEquals(expectedParams, actualParams)
    }

    private companion object {
        private const val CLIENT_SECRET = "seti_1CkiBMLENEVhOs7YMtUehLau_secret_sw1VaYPGZA"
    }
}
