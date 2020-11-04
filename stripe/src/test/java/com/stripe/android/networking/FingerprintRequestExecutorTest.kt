package com.stripe.android.networking

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.FingerprintDataFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FingerprintRequestExecutorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val fingerprintRequestFactory = FingerprintRequestFactory(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `execute() when successful should return non-empty values`() {
        testDispatcher.runBlockingTest {
            val remoteFingerprintData = createFingerprintRequestExecutor().execute(
                request = fingerprintRequestFactory.create(FINGERPRINT_DATA)
            )

            assertThat(remoteFingerprintData?.guid)
                .isNotEmpty()
            assertThat(remoteFingerprintData?.muid)
                .isNotEmpty()
            assertThat(remoteFingerprintData?.sid)
                .isNotEmpty()
        }
    }

    @Test
    fun `execute() when successful should return null`() {
        val request = fingerprintRequestFactory.create(FINGERPRINT_DATA)
        val connection = mock<StripeConnection>().also {
            whenever(it.responseCode).thenReturn(500)
        }

        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request))
                .thenReturn(connection)
        }

        testDispatcher.runBlockingTest {
            assertThat(
                createFingerprintRequestExecutor(connectionFactory = connectionFactory)
                    .execute(request = request)
            ).isNull()
        }
    }

    @Test
    fun `execute() when connection exception should return null`() {
        val request = fingerprintRequestFactory.create(FINGERPRINT_DATA)
        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request)).thenThrow(IOException())
        }

        testDispatcher.runBlockingTest {
            assertThat(
                createFingerprintRequestExecutor(connectionFactory = connectionFactory)
                    .execute(request = request)
            ).isNull()
        }
    }

    private fun createFingerprintRequestExecutor(
        connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) = FingerprintRequestExecutor.Default(
        connectionFactory = connectionFactory,
        workContext = testDispatcher
    )

    private companion object {
        private val FINGERPRINT_DATA = FingerprintDataFixtures.create()
    }
}
