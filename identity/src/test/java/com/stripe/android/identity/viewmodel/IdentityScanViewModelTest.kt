package com.stripe.android.identity.viewmodel

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class IdentityScanViewModelTest {
    private val mockFpsTracker: FPSTracker = mock()
    private val mockIdentityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory = mock()
    private val mockPageAndModelFiles = mock<IdentityViewModel.PageAndModelFiles> {
        on { page }.then { mock<VerificationPage>() }
        on { idDetectorFile }.then { mock<File>() }
        on { faceDetectorFile }.then { mock<File>() }
    }

    private val mockCameraManagerWithoutAdapter = mock<IdentityCameraManager> {
        on { cameraAdapter }.then { null }
    }

    private val mockCameraManagerWithAdapter = mock<IdentityCameraManager> {
        on { requireCameraAdapter() }.then { mock<CameraAdapter<CameraPreviewImage<Bitmap>>>() }
    }

    val viewModel = object : IdentityScanViewModel(
        ApplicationProvider.getApplicationContext(),
        mockFpsTracker,
        mockIdentityAnalyticsRequestFactory,
        mock(),
        mock(),
        mock()
    ) {
        override val scanFeedback = MutableStateFlow(null)
    }

    @Test
    fun testFpsTrackedOnInterimResult() = runBlocking {
        viewModel.cameraManager = mock()
        viewModel.onInterimResult(
            IdentityAggregator.InterimResult(mock<IdentityScanState.Initial>())
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

        verify(mockFpsTracker).reportAndReset(
            eq(IdentityAnalyticsRequestFactory.TYPE_SELFIE)
        )
    }

    @Test
    fun testNullIdentityScanFlowWhenStopScan() {
        viewModel.identityScanFlow = null
        viewModel.stopScan(mock())
        verify(mockIdentityAnalyticsRequestFactory).genericError(anyOrNull(), any())
    }

    @Test
    fun testNullCameraAdapterWhenStopScan() {
        viewModel.identityScanFlow = mock()
        viewModel.initializeScanFlowAndUpdateState(
            mockPageAndModelFiles,
            mockCameraManagerWithoutAdapter
        )
        viewModel.stopScan(mock())
        verify(mockIdentityAnalyticsRequestFactory).genericError(anyOrNull(), any())
    }

    @Test
    fun testValidStateWhenStopScan() {
        viewModel.initializeScanFlowAndUpdateState(
            mockPageAndModelFiles,
            mockCameraManagerWithAdapter
        )
        viewModel.stopScan(mock())
        verifyNoInteractions(mockIdentityAnalyticsRequestFactory)
    }
}
