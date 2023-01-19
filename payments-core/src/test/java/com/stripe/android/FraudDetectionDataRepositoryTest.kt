package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.networking.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.networking.FraudDetectionData
import com.stripe.android.networking.FraudDetectionDataRequestFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection.HTTP_OK
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FraudDetectionDataRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @AfterTest
    fun after() {
        Stripe.advancedFraudSignalsEnabled = true
    }

    @Test
    fun `save() ➡ refresh() ➡ get() should return original object`() {
        val expectedFraudDetectionData = createFraudDetectionData(elapsedTime = -5L)
        val repository = DefaultFraudDetectionDataRepository(context)
        repository.save(expectedFraudDetectionData)
        repository.refresh()
        assertThat(repository.getCached())
            .isEqualTo(expectedFraudDetectionData)
    }

    @Test
    fun `get() when FraudDetectionData is expired should request new remote FraudDetectionData`() =
        runTest {
            val mockStripeNetworkClient = mock<StripeNetworkClient>()
            val expectedGUID = UUID.randomUUID().toString()
            val expectedMUID = UUID.randomUUID().toString()
            val expectedSID = UUID.randomUUID().toString()

            whenever(mockStripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(
                    code = HTTP_OK,
                    body =
                    """
                    {
                        "guid": "$expectedGUID",
                        "muid": "$expectedMUID",
                        "sid": "$expectedSID"
                    }
                    """.trimIndent()
                )
            )

            val repository = DefaultFraudDetectionDataRepository(
                localStore = DefaultFraudDetectionDataStore(
                    context,
                    testDispatcher
                ),
                fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
                stripeNetworkClient = mockStripeNetworkClient,
                workContext = testDispatcher
            )
            val expiredFraudDetectionData = createFraudDetectionData(elapsedTime = -60L)
            repository.save(expiredFraudDetectionData)
            repository.refresh()
            val actualFraudDetectionData = repository.getCached()

            assertThat(expiredFraudDetectionData.guid).isNotEqualTo(expectedGUID)
            assertThat(expiredFraudDetectionData.muid).isNotEqualTo(expectedMUID)
            assertThat(expiredFraudDetectionData.sid).isNotEqualTo(expectedSID)

            assertThat(requireNotNull(actualFraudDetectionData).guid).isEqualTo(expectedGUID)
            assertThat(actualFraudDetectionData.muid).isEqualTo(expectedMUID)
            assertThat(actualFraudDetectionData.sid).isEqualTo(expectedSID)
        }

    @Test
    fun `refresh() when advancedFraudSignals is disabled should not fetch FraudDetectionData`() =
        runTest {
            Stripe.advancedFraudSignalsEnabled = false
            val mockStripeNetworkClient = mock<StripeNetworkClient>()

            val store: FraudDetectionDataStore = mock()
            val fraudDetectionDataRequestFactory: FraudDetectionDataRequestFactory = mock()
            val repository = DefaultFraudDetectionDataRepository(
                localStore = store,
                fraudDetectionDataRequestFactory = fraudDetectionDataRequestFactory,
                stripeNetworkClient = mockStripeNetworkClient,
                workContext = testDispatcher
            )
            repository.refresh()

            verify(store, never()).get()
            verify(store, never()).save(any())
            verify(fraudDetectionDataRequestFactory, never()).create(any())
            verify(mockStripeNetworkClient, never()).executeRequest(any())
        }

    private companion object {
        fun createFraudDetectionData(elapsedTime: Long = 0L): FraudDetectionData {
            return FraudDetectionDataFixtures.create(
                Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
