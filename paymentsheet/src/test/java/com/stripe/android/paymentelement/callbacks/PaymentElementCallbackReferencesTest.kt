package com.stripe.android.paymentelement.callbacks

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test

class PaymentElementCallbackReferencesTest {
    @get:Rule
    val testRule = PaymentElementCallbackTestRule()

    @Test
    fun `On get with no callbacks available, should return null`() {
        assertThat(PaymentElementCallbackReferences["Key1"]).isNull()
    }

    @Test
    fun `On get with callbacks assigned to the given key, should return callbacks`() {
        val callbacks = createCallbacks()

        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = callbacks

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(callbacks)
    }

    @Test
    fun `On get with callbacks not assigned to a given key but has callbacks set, should return the first set`() {
        val initialRegisteredCallbacks = createCallbacks()

        PaymentElementCallbackReferences["Key2"] = initialRegisteredCallbacks
        PaymentElementCallbackReferences["Key3"] = createCallbacks()
        PaymentElementCallbackReferences["Key4"] = createCallbacks()

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(initialRegisteredCallbacks)
    }

    @Test
    fun `On get after remove for a given key, should return null`() {
        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = createCallbacks()

        PaymentElementCallbackReferences.remove(DEFAULT_TEST_KEY)

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `On get after clear for a given key, should return null`() {
        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = createCallbacks()

        PaymentElementCallbackReferences.clear()

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @OptIn(ExperimentalCustomPaymentMethodsApi::class)
    private fun createCallbacks(): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("Should not be called!")
            }
            .customPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called!")
            }
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called!")
            }
            .build()
    }

    private companion object {
        const val DEFAULT_TEST_KEY = "Key1"
    }
}
