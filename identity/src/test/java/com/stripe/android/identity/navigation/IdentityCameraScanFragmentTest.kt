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
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
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
    private val mockCameraViewModel = mock<CameraViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val idDetectorModelFile = MutableLiveData<File>()
    private val idDetectorModelError = MutableLiveData<Throwable>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.idDetectorModelFile).thenReturn(idDetectorModelFile)
        whenever(it.idDetectorModelError).thenReturn(idDetectorModelError)
    }

    private val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()

    private val testCameraPermissionEnsureable = object : CameraPermissionEnsureable {
        lateinit var onCameraReady: () -> Unit
        lateinit var onUserDeniedCameraPermission: () -> Unit

        override fun ensureCameraPermission(
            onCameraReady: () -> Unit,
            onUserDeniedCameraPermission: () -> Unit
        ) {
            this.onCameraReady = onCameraReady
            this.onUserDeniedCameraPermission = onUserDeniedCameraPermission
        }
    }

    internal class TestFragment(
        cameraPermissionEnsureable: CameraPermissionEnsureable,
        cameraViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory,
        private val cameraViewParam: CameraView
    ) : IdentityCameraScanFragment(
        cameraPermissionEnsureable, cameraViewModelFactory, identityViewModelFactory
    ) {
        var onCameraReadyCalled = false
        var onUserDeniedCameraPermissionCalled = false
        var currentState: IdentityScanState? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            cameraView = cameraViewParam
            return View(context)
        }

        override fun onCameraReady() {
            onCameraReadyCalled = true
        }

        override fun onUserDeniedCameraPermission() {
            onUserDeniedCameraPermissionCalled = true
        }

        override fun updateUI(identityScanState: IdentityScanState) {
            currentState = identityScanState
        }
    }

    @Test
    fun `when model file is ready scanflow is initialized and camera permission is requested`() {
        launchIDScanFragment(mockCameraPermissionEnsureable).onFragment {
            val mockFile = mock<File>()
            idDetectorModelFile.postValue(mockFile)

            verify(mockCameraViewModel).initializeScanFlow(same(mockFile))
            verify(mockCameraPermissionEnsureable).ensureCameraPermission(any(), any())
        }
    }

    @Test
    fun `when model file is not ready exception is thrown`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            assertFailsWith<InvalidResponseException> {
                idDetectorModelError.postValue(mock())
            }
        }
    }

    @Test
    fun `when camera permission granted onCameraReady is called`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            idDetectorModelFile.postValue(mock())
            testCameraPermissionEnsureable.onCameraReady()

            assertThat(it.cameraAdapter).isNotNull()
            assertThat(it.onCameraReadyCalled).isTrue()
        }
    }

    @Test
    fun `when camera permission denied onUserDeniedCameraPermissionCalled is called`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            idDetectorModelFile.postValue(mock())
            testCameraPermissionEnsureable.onUserDeniedCameraPermission()

            assertThat(it.cameraAdapter).isNotNull()
            assertThat(it.onUserDeniedCameraPermissionCalled).isTrue()
        }
    }

    @Test
    fun `when displayStateChanged updateUI is called`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            val newState = mock<IdentityScanState.Initial>()
            displayStateChanged.postValue((newState to mock()))

            assertThat(it.currentState).isEqualTo(newState)
        }
    }

    @Test
    fun `when finalResult is posted scan is stopped`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            finalResultLiveData.postValue(mock())

            verify(mockScanFlow).resetFlow()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when destroyed scanFlow is cancelled`() {
        launchIDScanFragment(testCameraPermissionEnsureable).moveToState(Lifecycle.State.DESTROYED)
        verify(mockScanFlow).cancelFlow()
    }

    private fun launchIDScanFragment(
        cameraPermissionEnsureable: CameraPermissionEnsureable
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestFragment(
            cameraPermissionEnsureable,
            viewModelFactoryFor(mockCameraViewModel),
            viewModelFactoryFor(mockIdentityViewModel),
            mockCameraView
        )
    }
}
