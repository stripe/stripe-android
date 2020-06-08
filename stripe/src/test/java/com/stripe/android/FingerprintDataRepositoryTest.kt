package com.stripe.android

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FingerprintDataRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)
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
            store = FingerprintDataStore.Default(
                context,
                testDispatcher
            ),
            fingerprintRequestFactory = FingerprintRequestFactory(context),
            fingerprintRequestExecutor = object : FingerprintRequestExecutor {
                override fun execute(
                    request: FingerprintRequest
                ) = MutableLiveData<FingerprintData?>().also {
                    it.value = expectedFingerprintData
                }
            }
        )
        repository.save(createFingerprintData(elapsedTime = -60L))
        repository.refresh()

        assertThat(repository.get())
            .isEqualTo(expectedFingerprintData)
    }

    @Test
    fun `isExpired() when less than 30 minutes have elapsed should return false`() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(29L)
            )
        ).isFalse()
    }

    @Test
    fun `isExpired() when more than 30 minutes have elapsed should return true`() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(31L)
            )
        ).isTrue()
    }

    @Test
    fun `refresh() when advancedFraudSignals is disabled should not fetch FingerprintData`() {
        Stripe.advancedFraudSignalsEnabled = false

        val store: FingerprintDataStore = mock()
        val fingerprintRequestFactory: FingerprintRequestFactory = mock()
        val repository = FingerprintDataRepository.Default(
            store = store,
            fingerprintRequestFactory = fingerprintRequestFactory,
            fingerprintRequestExecutor = fingerprintRequestExecutor,
            coroutineScope = testScope
        )
        repository.refresh()

        verify(store, never()).get()
        verify(store, never()).save(any())
        verify(fingerprintRequestFactory, never()).create(any())
        verify(fingerprintRequestExecutor, never()).execute(any())
    }

    private companion object {
        fun createFingerprintData(elapsedTime: Long = 0L): FingerprintData {
            return FingerprintData(
                guid = UUID.randomUUID().toString(),
                timestamp = Calendar.getInstance().timeInMillis +
                    TimeUnit.MINUTES.toMillis(elapsedTime)
            )
        }
    }
}
