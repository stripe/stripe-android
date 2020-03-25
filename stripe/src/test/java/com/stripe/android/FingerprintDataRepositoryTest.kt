package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintDataRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fingerprintRequestExecutor: FingerprintRequestExecutor = mock()
    private val requestExecutorCallback: KArgumentCaptor<(FingerprintData?) -> Unit> = argumentCaptor()

    @Test
    fun roundtrip_shouldReturnOriginalObject() {
        val expectedFingerprintData = createFingerprintData(elapsedTime = -5L)
        val repository = FingerprintDataRepository.Default(context)
        var actualFingerprintData: FingerprintData? = null
        repository.save(expectedFingerprintData)
        repository.get().observeForever {
            actualFingerprintData = it
        }
        assertThat(actualFingerprintData)
            .isEqualTo(expectedFingerprintData)
    }

    @Test
    fun get_whenFingerprintDataIsExpired_shouldRequestNewFingerprintDataRemotely() {
        var expectedFingerprintData: FingerprintData? = null

        doNothing().whenever(fingerprintRequestExecutor).execute(any(), any())

        val repository = FingerprintDataRepository.Default(
            store = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context),
            fingerprintRequestExecutor = fingerprintRequestExecutor
        )
        repository.save(createFingerprintData(elapsedTime = -60L))
        repository.get().observeForever {
            expectedFingerprintData = it
        }

        val remoteFingerprintData = createFingerprintData()
        verify(fingerprintRequestExecutor).execute(
            any(),
            requestExecutorCallback.capture()
        )
        requestExecutorCallback.firstValue.invoke(remoteFingerprintData)

        assertThat(expectedFingerprintData)
            .isEqualTo(remoteFingerprintData)
    }

    @Test
    fun isExpired_whenFewerThan30MinutesElapsed_shouldReturnFalse() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(29L)
            )
        ).isFalse()
    }

    @Test
    fun isExpired_whenGreaterThan30MinutesElapsed_shouldReturnFalse() {
        val fingerprintData = createFingerprintData()
        assertThat(
            fingerprintData.isExpired(
                currentTime = fingerprintData.timestamp + TimeUnit.MINUTES.toMillis(31L)
            )
        ).isTrue()
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
