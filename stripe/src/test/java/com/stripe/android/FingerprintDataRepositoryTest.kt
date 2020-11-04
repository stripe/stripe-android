package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.networking.FingerprintRequest
import com.stripe.android.networking.FingerprintRequestExecutor
import com.stripe.android.networking.FingerprintRequestFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FingerprintDataRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()
    private val fingerprintRequestExecutor: FingerprintRequestExecutor = mock()

    @AfterTest
    fun after() {
        Stripe.advancedFraudSignalsEnabled = true
    }

    @Test
    fun `save() ➡ refresh() ➡ get() should return original object`() {
        val expectedFingerprintData = createFingerprintData(elapsedTime = -5L)
        val repository = FingerprintDataRepository.Default(context)
        repository.save(expectedFingerprintData)
        repository.refresh()
        assertThat(repository.get())
            .isEqualTo(expectedFingerprintData)
    }

    @Test
    fun `get() when FingerprintData is expired should request new remote FingerprintData`() {
        val expectedFingerprintData = createFingerprintData()
        val repository = FingerprintDataRepository.Default(
            localStore = FingerprintDataStore.Default(
                context,
                testDispatcher
            ),
            fingerprintRequestFactory = FingerprintRequestFactory(context),
            fingerprintRequestExecutor = object : FingerprintRequestExecutor {
                override suspend fun execute(request: FingerprintRequest) = expectedFingerprintData
            },
            workContext = testDispatcher
        )
        repository.save(createFingerprintData(elapsedTime = -60L))
        repository.refresh()
        val actualFingerprintData = repository.get()

        assertThat(actualFingerprintData)
            .isEqualTo(expectedFingerprintData)
    }

    @Test
    fun `refresh() when advancedFraudSignals is disabled should not fetch FingerprintData`() = testDispatcher.runBlockingTest {
        Stripe.advancedFraudSignalsEnabled = false

        val store: FingerprintDataStore = mock()
        val fingerprintRequestFactory: FingerprintRequestFactory = mock()
        val repository = FingerprintDataRepository.Default(
            localStore = store,
            fingerprintRequestFactory = fingerprintRequestFactory,
            fingerprintRequestExecutor = fingerprintRequestExecutor,
            workContext = testDispatcher
        )
        repository.refresh()

        verify(store, never()).get()
        verify(store, never()).save(any())
        verify(fingerprintRequestFactory, never()).create(any())
        verify(fingerprintRequestExecutor, never()).execute(any())
    }

    private companion object {
        fun createFingerprintData(elapsedTime: Long = 0L): FingerprintData {
            return FingerprintDataFixtures.create(
                Calendar.getInstance().timeInMillis + TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
