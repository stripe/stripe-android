package com.stripe.android.paymentelement.callbacks

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentElementCallbacksTest {

    @Test
    fun `build succeeds when only createIntentCallback is set`() {
        val callback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val callbacks = PaymentElementCallbacks.Builder()
            .createIntentCallback(callback)
            .build()

        assertThat(callbacks.createIntentCallback).isEqualTo(callback)
        assertThat(callbacks.createIntentWithConfirmationTokenCallback).isNull()
    }

    @Test
    fun `build succeeds when only createIntentWithConfirmationTokenCallback is set`() {
        val callback = CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val callbacks = PaymentElementCallbacks.Builder()
            .createIntentCallback(callback)
            .build()

        assertThat(callbacks.createIntentCallback).isNull()
        assertThat(callbacks.createIntentWithConfirmationTokenCallback).isEqualTo(callback)
    }

    @Test
    fun `build throws IllegalArgumentException when both callbacks are non-null`() {
        val intentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val confirmationTokenCallback = CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val exception = kotlin.runCatching {
            PaymentElementCallbacks.Builder()
                .createIntentCallback(intentCallback)
                .createIntentCallback(confirmationTokenCallback)
                .build()
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception?.message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    @OptIn(SharedPaymentTokenSessionPreview::class)
    fun `build throws when both CreateIntentCallback and PreparePaymentMethodHandler are non-null`() {
        val intentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val preparePaymentMethodHandler = PreparePaymentMethodHandler { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val exception = kotlin.runCatching {
            PaymentElementCallbacks.Builder()
                .createIntentCallback(intentCallback)
                .preparePaymentMethodHandler(preparePaymentMethodHandler)
                .build()
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception?.message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    @OptIn(SharedPaymentTokenSessionPreview::class)
    fun `build throws when both ConfirmationTokenCallback and PreparePaymentMethodHandler are non-null`() {
        val confirmationTokenCallback = CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val preparePaymentMethodHandler = PreparePaymentMethodHandler { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val exception = kotlin.runCatching {
            PaymentElementCallbacks.Builder()
                .createIntentCallback(confirmationTokenCallback)
                .preparePaymentMethodHandler(preparePaymentMethodHandler)
                .build()
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception?.message).isEqualTo(ERROR_MESSAGE)
    }

    companion object {

        private const val ERROR_MESSAGE =
            "Only one of createIntentCallback, createIntentWithConfirmationTokenCallback " +
                "or preparePaymentMethodHandler can be set"
    }
}
