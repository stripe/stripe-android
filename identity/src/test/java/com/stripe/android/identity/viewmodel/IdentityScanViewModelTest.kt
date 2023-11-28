package com.stripe.android.identity.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
internal class IdentityScanViewModelTest {
    private val mockFpsTracker: FPSTracker = mock()
    private val mockIdentityRepository: IdentityRepository = mock()
    private val mockIdentityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewModel = IdentityScanViewModel(
        WeakReference(ApplicationProvider.getApplicationContext()),
        mockFpsTracker,
        mockIdentityRepository,
        mockIdentityAnalyticsRequestFactory,
        mock(),
        mock()
    )

    @Test
    fun testFpsTrackedOnInterimResult() = runBlocking {
        viewModel.onInterimResult(
            IdentityAggregator.InterimResult(mock())
        )
        verify(mockFpsTracker).trackFrame()
    }

    @Test
    fun testFinishedResult() = runBlocking {
        val result = IdentityAggregator.FinalResult(
            mock(),
            mock(),
            identityState = mock<IdentityScanState.Finished>()
        )

        viewModel.onResult(result)

        assertThat(
            (viewModel.scannerState.value as IdentityScanViewModel.State.Scanned).result
        ).isSameInstanceAs(result)
    }

    @Test
    fun testTimeoutResultForDoc() = runBlocking {
        val resultType = IdentityScanState.ScanType.DOC_FRONT
        val result = IdentityAggregator.FinalResult(
            mock(),
            result = mock<IDDetectorOutput>(),
            identityState = IdentityScanState.TimeOut(
                resultType,
                mock()
            )
        )

        viewModel.onResult(result)

        assertThat((viewModel.scannerState.value as IdentityScanViewModel.State.Timeout).fromSelfie).isFalse()
        verify(mockIdentityAnalyticsRequestFactory).documentTimeout(eq(resultType))
        verify(mockIdentityRepository).sendAnalyticsRequest(anyOrNull())

        verify(mockFpsTracker).reportAndReset(
            eq(IdentityAnalyticsRequestFactory.TYPE_DOCUMENT)
        )
    }

    @Test
    fun testTimeoutResultForSelfie() = runBlocking {
        val resultType = IdentityScanState.ScanType.SELFIE
        val result = IdentityAggregator.FinalResult(
            mock(),
            result = mock<FaceDetectorOutput>(),
            identityState = IdentityScanState.TimeOut(
                resultType,
                mock()
            )
        )

        viewModel.onResult(result)

        assertThat((viewModel.scannerState.value as IdentityScanViewModel.State.Timeout).fromSelfie).isTrue()
        verify(mockIdentityAnalyticsRequestFactory).selfieTimeout()
        verify(mockIdentityRepository).sendAnalyticsRequest(anyOrNull())

        verify(mockFpsTracker).reportAndReset(
            eq(IdentityAnalyticsRequestFactory.TYPE_SELFIE)
        )
    }
}
