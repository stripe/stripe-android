package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networking.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.networking.FraudDetectionData
import com.stripe.android.networking.FraudDetectionDataRequest
import com.stripe.android.networking.FraudDetectionDataRequestExecutor
import com.stripe.android.networking.FraudDetectionDataRequestFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FraudDetectionDataRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()
    private val fraudDetectionDataRequestExecutor: FraudDetectionDataRequestExecutor = mock()

    @AfterTest
    fun after() {
        Stripe.advancedFraudSignalsEnabled = true
        testDispatcher.cleanupTestCoroutines()
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
    fun `get() when FraudDetectionData is expired should request new remote FraudDetectionData`() {
        val expectedFraudDetectionData = createFraudDetectionData()
        val repository = DefaultFraudDetectionDataRepository(
            localStore = DefaultFraudDetectionDataStore(
                context,
                testDispatcher
            ),
            fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
            fraudDetectionDataRequestExecutor = object : FraudDetectionDataRequestExecutor {
                override suspend fun execute(request: FraudDetectionDataRequest) = expectedFraudDetectionData
            },
            workContext = testDispatcher
        )
        repository.save(createFraudDetectionData(elapsedTime = -60L))
        repository.refresh()
        val actualFraudDetectionData = repository.getCached()

        assertThat(actualFraudDetectionData)
            .isEqualTo(expectedFraudDetectionData)
    }

    @Test
    fun `refresh() when advancedFraudSignals is disabled should not fetch FraudDetectionData`() = testDispatcher.runBlockingTest {
        Stripe.advancedFraudSignalsEnabled = false

        val store: FraudDetectionDataStore = mock()
        val fraudDetectionDataRequestFactory: FraudDetectionDataRequestFactory = mock()
        val repository = DefaultFraudDetectionDataRepository(
            localStore = store,
            fraudDetectionDataRequestFactory = fraudDetectionDataRequestFactory,
            fraudDetectionDataRequestExecutor = fraudDetectionDataRequestExecutor,
            workContext = testDispatcher
        )
        repository.refresh()

        verify(store, never()).get()
        verify(store, never()).save(any())
        verify(fraudDetectionDataRequestFactory, never()).create(any())
        verify(fraudDetectionDataRequestExecutor, never()).execute(any())
    }

    private companion object {
        fun createFraudDetectionData(elapsedTime: Long = 0L): FraudDetectionData {
            return FraudDetectionDataFixtures.create(
                Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
