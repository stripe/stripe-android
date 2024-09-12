package com.stripe.android

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.DefaultGooglePayAvailabilityClient
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test

class DefaultGooglePayAvailabilityClientTest {
    @Test
    fun `On is ready, should call payments client`() = runTest {
        val paymentsClient = createPaymentsClient()

        val client = DefaultGooglePayAvailabilityClient(
            paymentsClient = paymentsClient,
        )

        val isReadyToPayRequest = createIsReadyToPayRequest()

        client.isReady(isReadyToPayRequest)

        verify(paymentsClient).isReadyToPay(isReadyToPayRequest)
    }

    @Test
    fun `On is ready is true from payments client, should return true`() = runTest {
        val paymentsClient = createPaymentsClient(isReady = true)

        val client = DefaultGooglePayAvailabilityClient(
            paymentsClient = paymentsClient,
        )

        assertThat(client.isReady(createIsReadyToPayRequest())).isTrue()
    }

    @Test
    fun `On is ready is false from payments client, should return false`() = runTest {
        val paymentsClient = createPaymentsClient(isReady = false)

        val client = DefaultGooglePayAvailabilityClient(
            paymentsClient = paymentsClient,
        )

        assertThat(client.isReady(createIsReadyToPayRequest())).isFalse()
    }

    private fun createPaymentsClient(isReady: Boolean = false): PaymentsClient {
        return mock<PaymentsClient> {
            on { isReadyToPay(any()) } doReturn Tasks.forResult(isReady)
        }
    }

    private fun createIsReadyToPayRequest(): IsReadyToPayRequest {
        return mock<IsReadyToPayRequest>()
    }
}
