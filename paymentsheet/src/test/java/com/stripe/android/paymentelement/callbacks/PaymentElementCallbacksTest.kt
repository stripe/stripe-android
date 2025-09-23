package com.stripe.android.paymentelement.callbacks

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
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
            .createIntentWithConfirmationTokenCallback(callback)
            .build()

        assertThat(callbacks.createIntentCallback).isNull()
        assertThat(callbacks.createIntentWithConfirmationTokenCallback).isEqualTo(callback)
    }

    @Test
    fun `build succeeds when both callbacks are null`() {
        val callbacks = PaymentElementCallbacks.Builder()
            .createIntentCallback(null)
            .createIntentWithConfirmationTokenCallback(null)
            .build()

        assertThat(callbacks.createIntentCallback).isNull()
        assertThat(callbacks.createIntentWithConfirmationTokenCallback).isNull()
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
                .createIntentWithConfirmationTokenCallback(confirmationTokenCallback)
                .build()
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception?.message).isEqualTo(
            "At most one of createIntentCallback and createIntentWithConfirmationTokenCallback" +
                "can be non-null"
        )
    }

    @Test
    fun `build throws IllegalArgumentException when setting non-null callback after setting the other non-null callback`() {
        val intentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val confirmationTokenCallback = CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        // Test setting intentCallback first, then confirmationTokenCallback
        val exception1 = kotlin.runCatching {
            PaymentElementCallbacks.Builder()
                .createIntentCallback(intentCallback)
                .createIntentWithConfirmationTokenCallback(confirmationTokenCallback)
                .build()
        }.exceptionOrNull()

        assertThat(exception1).isInstanceOf(IllegalArgumentException::class.java)

        // Test setting confirmationTokenCallback first, then intentCallback
        val exception2 = kotlin.runCatching {
            PaymentElementCallbacks.Builder()
                .createIntentWithConfirmationTokenCallback(confirmationTokenCallback)
                .createIntentCallback(intentCallback)
                .build()
        }.exceptionOrNull()

        assertThat(exception2).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `build succeeds when setting non-null callback after setting the other to null`() {
        val intentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        val confirmationTokenCallback = CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Success("pi_123_secret_456")
        }

        // Set intentCallback to non-null, then set confirmationTokenCallback to non-null but intentCallback to null
        val callbacks1 = PaymentElementCallbacks.Builder()
            .createIntentCallback(intentCallback)
            .createIntentCallback(null)
            .createIntentWithConfirmationTokenCallback(confirmationTokenCallback)
            .build()

        assertThat(callbacks1.createIntentCallback).isNull()
        assertThat(callbacks1.createIntentWithConfirmationTokenCallback).isEqualTo(confirmationTokenCallback)

        // Set confirmationTokenCallback to non-null, then set intentCallback to non-null but confirmationTokenCallback to null
        val callbacks2 = PaymentElementCallbacks.Builder()
            .createIntentWithConfirmationTokenCallback(confirmationTokenCallback)
            .createIntentWithConfirmationTokenCallback(null)
            .createIntentCallback(intentCallback)
            .build()

        assertThat(callbacks2.createIntentCallback).isEqualTo(intentCallback)
        assertThat(callbacks2.createIntentWithConfirmationTokenCallback).isNull()
    }
}