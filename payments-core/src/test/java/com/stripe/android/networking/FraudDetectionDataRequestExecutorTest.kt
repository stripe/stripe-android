package com.stripe.android.networking

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.FraudDetectionDataFixtures
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FraudDetectionDataRequestExecutorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(
        context = ApplicationProvider.getApplicationContext()
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `execute() when successful should return non-empty values`() {
        testDispatcher.runBlockingTest {
            val remoteFraudDetectionData = requireNotNull(
                createFraudDetectionDataRequestExecutor().execute(
                    request = fraudDetectionDataRequestFactory.create(FRAUD_DETECTION_DATA)
                )
            )

            assertThat(remoteFraudDetectionData.guid)
                .isNotEmpty()
            assertThat(remoteFraudDetectionData.muid)
                .isNotEmpty()
            assertThat(remoteFraudDetectionData.sid)
                .isNotEmpty()
        }
    }

    @Test
    fun `execute() when successful should return null`() {
        val request = fraudDetectionDataRequestFactory.create(FRAUD_DETECTION_DATA)
        val connection = mock<StripeConnection<String>>().also {
            whenever(it.responseCode).thenReturn(500)
        }

        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request))
                .thenReturn(connection)
        }

        testDispatcher.runBlockingTest {
            assertThat(
                createFraudDetectionDataRequestExecutor(connectionFactory = connectionFactory)
                    .execute(request = request)
            ).isNull()
        }
    }

    @Test
    fun `execute() when connection exception should return null`() {
        val request = fraudDetectionDataRequestFactory.create(FRAUD_DETECTION_DATA)
        val connectionFactory = mock<ConnectionFactory>().also {
            whenever(it.create(request)).thenThrow(IOException())
        }

        testDispatcher.runBlockingTest {
            assertThat(
                createFraudDetectionDataRequestExecutor(connectionFactory = connectionFactory)
                    .execute(request = request)
            ).isNull()
        }
    }

    private fun createFraudDetectionDataRequestExecutor(
        connectionFactory: ConnectionFactory = ConnectionFactory.Default
    ) = DefaultFraudDetectionDataRequestExecutor(
        connectionFactory = connectionFactory,
        workContext = testDispatcher
    )

    private companion object {
        private val FRAUD_DETECTION_DATA = FraudDetectionDataFixtures.create()
    }
}
