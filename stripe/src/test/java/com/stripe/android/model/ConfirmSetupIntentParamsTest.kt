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
    fun toBuilder_withPaymentMethodId_shouldCreateEqualObject() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            "pm_123", "client_secret", "return_url")
        assertEquals(confirmSetupIntentParams,
            confirmSetupIntentParams.toBuilder().build())
    }

    @Test
    fun toBuilder_withPaymentMethodCreateParams_shouldCreateEqualObject() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            "client_secret", "return_url")
        assertEquals(confirmSetupIntentParams,
            confirmSetupIntentParams.toBuilder().build())
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
        val params = ConfirmSetupIntentParams(
            clientSecret = CLIENT_SECRET,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
            useStripeSdk = true
        ).toParamMap()
        assertEquals(
            MandateDataParams().toParamMap(),
            params[ConfirmStripeIntentParams.PARAM_MANDATE_DATA]
        )
    }

    @Test
    fun create_withSepaDebitPaymentMethodParams_shouldUseMandateDataIfSpecified() {
        val params = ConfirmSetupIntentParams(
            clientSecret = CLIENT_SECRET,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
            mandateData = MandateDataParamsFixtures.DEFAULT,
            useStripeSdk = true
        ).toParamMap()
        assertEquals(
            MandateDataParamsFixtures.DEFAULT.toParamMap(),
            params[ConfirmStripeIntentParams.PARAM_MANDATE_DATA]
        )
    }

    private companion object {
        private const val CLIENT_SECRET = "seti_1CkiBMLENEVhOs7YMtUehLau_secret_sw1VaYPGZA"
    }
}
