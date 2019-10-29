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
        assertNull(params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA])
        assertEquals("pm_12345",
            params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID])
    }

    @Test
    fun create_withPaymentMethodCreateParams_shouldPopulateParamMapCorrectly() {
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            "client_secret", null
        )
        val params = confirmSetupIntentParams.toParamMap()
        assertNull(params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID])
        val paymentMethodData =
            params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA] as Map<String, Any>
        assertEquals("card", paymentMethodData["type"])
        assertNotNull(paymentMethodData["card"])
    }

    @Test
    fun create_withoutPaymentMethod() {
        val params = ConfirmSetupIntentParams.createWithoutPaymentMethod("client_secret")
        assertNull(params.paymentMethodCreateParams)
        assertNull(params.paymentMethodId)
    }
}
