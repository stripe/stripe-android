package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestExecutorTest {
    private val fingerprintRequestFactory = FingerprintRequestFactory(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun execute_whenSuccessful_shouldReturnResponseWithUuid() {
        createFingerprintRequestExecutor().execute(
            request = fingerprintRequestFactory.create(GUID.toString())
        ) {
            assertThat(UUID.fromString(it?.guid))
                .isNotNull()
        }
    }

    @Test
    fun execute_whenErrorResponse_shouldInvokeCallback() {
        var callbackCount = 0

        val request = fingerprintRequestFactory.create(GUID.toString())
        val connection = mock<StripeConnection>().also {
            whenever(it.responseCode).thenReturn(500)
        }

        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request))
                .thenReturn(connection)
        }

        createFingerprintRequestExecutor(connectionFactory = connectionFactory)
            .execute(
                request = request,
                callback = {
                    callbackCount++
                }
            )

        assertThat(callbackCount)
            .isEqualTo(1)
    }

    @Test
    fun execute_whenConnectionException_shouldInvokeCallback() {
        var callbackCount = 0

        val request = fingerprintRequestFactory.create(GUID.toString())
        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request)).thenThrow(IOException())
        }
        createFingerprintRequestExecutor(connectionFactory = connectionFactory)
            .execute(
                request = request,
                callback = {
                    callbackCount++
                }
            )

        assertThat(callbackCount)
            .isEqualTo(1)
    }

    private fun createFingerprintRequestExecutor(
        connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) = FingerprintRequestExecutor.Default(
        workScope = MainScope(),
        connectionFactory = connectionFactory
    )

    private companion object {
        private val GUID = UUID.randomUUID()
    }
}
