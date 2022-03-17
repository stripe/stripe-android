package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper.idleMainLooper
import java.io.File
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class IdentityCameraScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockPreviewFrame = mock<FrameLayout>()
    private val mockCameraView = mock<CameraView>().also {
        whenever(it.previewFrame).thenReturn(mockPreviewFrame)
    }

    private val finalResultLiveData = MutableLiveData<IDDetectorAggregator.FinalResult>()
    private val displayStateChanged = MutableLiveData<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val idDetectorModelFile = MutableLiveData<Resource<File>>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.idDetectorModelFile).thenReturn(idDetectorModelFile)
    }

    internal class TestFragment(
        identityScanViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory,
        private val cameraViewParam: CameraView
    ) : IdentityCameraScanFragment(
        identityScanViewModelFactory, identityViewModelFactory
    ) {
        var currentState: IdentityScanState? = null
        var onCameraReadyIsCalled = false

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            cameraView = cameraViewParam
            return View(context)
        }

        override val headerTitleRes = R.string.front_of_dl
        override val messageRes = R.string.position_dl_front

        override fun onCameraReady() {
            onCameraReadyIsCalled = true
        }

        override fun updateUI(identityScanState: IdentityScanState) {
            currentState = identityScanState
        }
    }

    @Test
    fun `when model file is not ready exception is thrown`() {
        launchIDScanFragment().onFragment {
            assertFailsWith<InvalidResponseException> {
                idDetectorModelFile.postValue(Resource.error())
            }
        }
    }

    @Test
    fun `when model file is ready onCameraReady is called`() {
        launchIDScanFragment().onFragment {
            idDetectorModelFile.postValue(Resource.success(mock()))
            idleMainLooper()

            assertThat(it.cameraAdapter).isNotNull()
            assertThat(it.onCameraReadyIsCalled).isTrue()
        }
    }

    @Test
    fun `when displayStateChanged updateUI is called`() {
        launchIDScanFragment().onFragment {
            val newState = mock<IdentityScanState.Initial>()
            displayStateChanged.postValue((newState to mock()))

            assertThat(it.currentState).isEqualTo(newState)
        }
    }

    @Test
    fun `when finalResult is posted scan is stopped`() {
        launchIDScanFragment().onFragment {
            finalResultLiveData.postValue(mock())

            verify(mockScanFlow).resetFlow()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when destroyed scanFlow is cancelled`() {
        launchIDScanFragment().moveToState(Lifecycle.State.DESTROYED)
        verify(mockScanFlow).cancelFlow()
    }

    private fun launchIDScanFragment() = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestFragment(
            viewModelFactoryFor(mockIdentityScanViewModel),
            viewModelFactoryFor(mockIdentityViewModel),
            mockCameraView
        )
    }
}
