package com.stripe.android.model

import java.util.Objects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            PM_CREATE_PARAMS,
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
            PM_CREATE_PARAMS, "client_secret", "return_url")
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
            PM_CREATE_PARAMS,
            "client_secret", null
        )
        val params = confirmSetupIntentParams.toParamMap()
        assertNull(params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID])
        val paymentMethodData = Objects.requireNonNull<Any>(
            params[ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA]) as Map<String, Any>
        assertEquals("card", paymentMethodData["type"])
        assertNotNull(paymentMethodData["card"])
    }

    companion object {

        private val PM_CREATE_PARAMS = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.Builder()
                .setNumber("4242424242424242")
                .setExpiryMonth(1)
                .setExpiryYear(2024)
                .setCvc("111")
                .build(), null
        )
    }
}
