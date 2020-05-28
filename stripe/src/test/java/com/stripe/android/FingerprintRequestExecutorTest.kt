package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FingerprintRequestExecutorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val fingerprintRequestFactory = FingerprintRequestFactory(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `execute() when successful should return response with UUID`() {
        var remoteFingerprintData: FingerprintData? = null
        createFingerprintRequestExecutor().execute(
            request = fingerprintRequestFactory.create(GUID.toString())
        ).observeForever {
            remoteFingerprintData = it
        }

        assertThat(UUID.fromString(remoteFingerprintData?.guid))
            .isNotNull()
    }

    @Test
    fun `execute() when successful should return null`() {
        val request = fingerprintRequestFactory.create(GUID.toString())
        val connection = mock<StripeConnection>().also {
            whenever(it.responseCode).thenReturn(500)
        }

        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request))
                .thenReturn(connection)
        }

        var callbackCount = 0
        var remoteFingerprintData: FingerprintData? = null
        createFingerprintRequestExecutor(connectionFactory = connectionFactory)
            .execute(request = request)
            .observeForever {
                callbackCount++
                remoteFingerprintData = it
            }

        assertThat(callbackCount)
            .isEqualTo(1)
        assertThat(remoteFingerprintData)
            .isNull()
    }

    @Test
    fun `execute() when connection exception should return null`() {
        val request = fingerprintRequestFactory.create(GUID.toString())
        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request)).thenThrow(IOException())
        }

        var callbackCount = 0
        var remoteFingerprintData: FingerprintData? = null
        createFingerprintRequestExecutor(connectionFactory = connectionFactory)
            .execute(request = request)
            .observeForever {
                callbackCount++
                remoteFingerprintData = it
            }

        assertThat(callbackCount)
            .isEqualTo(1)
        assertThat(remoteFingerprintData)
            .isNull()
    }

    private fun createFingerprintRequestExecutor(
        connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) = FingerprintRequestExecutor.Default(
        dispatcher = testDispatcher,
        connectionFactory = connectionFactory
    )

    private companion object {
        private val GUID = UUID.randomUUID()
    }
}
