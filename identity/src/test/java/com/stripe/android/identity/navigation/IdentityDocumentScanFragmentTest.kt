package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper.idleMainLooper
import java.io.File
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class IdentityDocumentScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockPreviewFrame = mock<FrameLayout>()
    private val mockCameraView = mock<CameraView>().also {
        whenever(it.previewFrame).thenReturn(mockPreviewFrame)
    }

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<Pair<VerificationPage, File>>>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.pageAndModel).thenReturn(mockPageAndModel)
    }

    internal class TestFragment(
        identityScanViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory,
        private val cameraViewParam: CameraView
    ) : IdentityDocumentScanFragment(
        identityScanViewModelFactory, identityViewModelFactory
    ) {
        override val fragmentId = R.id.IDScanFragment
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

        override fun onCameraReady() {
            onCameraReadyIsCalled = true
        }

        override fun updateUI(identityScanState: IdentityScanState) {
            currentState = identityScanState
        }
    }

    @Test
    fun `when viewCreated uploadedState is reset`() {
        launchTestFragment().onFragment {
            verify(mockIdentityViewModel).resetDocumentUploadedState()
        }
    }

    @Test
    fun `when page or model is not ready exception is thrown`() {
        launchTestFragment().onFragment {
            assertFailsWith<InvalidResponseException> {
                mockPageAndModel.postValue(Resource.error())
            }
        }
    }

    @Test
    fun `when page and model are ready onCameraReady is called`() {
        launchTestFragment().onFragment {
            mockPageAndModel.postValue(Resource.success(Pair(SUCCESS_VERIFICATION_PAGE, mock())))

            idleMainLooper()

            assertThat(it.cameraAdapter).isNotNull()
            assertThat(it.onCameraReadyIsCalled).isTrue()
        }
    }

    @Test
    fun `when displayStateChanged updateUI is called`() {
        launchTestFragment().onFragment {
            val newState = mock<IdentityScanState.Initial>()
            displayStateChanged.postValue((newState to mock()))

            assertThat(it.currentState).isEqualTo(newState)
        }
    }

    @Test
    fun `when finalResult is posted with Finished observes for verification page and scan is stopped`() {
        launchTestFragment().onFragment { testFragment ->
            finalResultLiveData.postValue(
                mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
            )

            verify(mockIdentityViewModel).observeForVerificationPage(
                same(testFragment.viewLifecycleOwner),
                any(),
                any()
            )

            verify(mockScanFlow).resetFlow()
            assertThat(testFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when finalResult is posted with Timeout navigates to couldNotCaptureFragment`() {
        launchTestFragment().onFragment { testFragment ->
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.IDScanFragment)
            Navigation.setViewNavController(
                testFragment.requireView(),
                navController
            )

            whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)

            finalResultLiveData.postValue(
                mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.TimeOut>())
                }
            )

            val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
            verify(mockIdentityViewModel).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            successCaptor.firstValue(SUCCESS_VERIFICATION_PAGE)

            verify(mockScanFlow).resetFlow()
            assertThat(testFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.couldNotCaptureFragment)
            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [CouldNotCaptureFragment.ARG_COULD_NOT_CAPTURE_SCAN_TYPE]
            ).isEqualTo(IdentityScanState.ScanType.ID_FRONT)
        }
    }

    @Test
    fun `when destroyed scanFlow is cancelled`() {
        launchTestFragment().moveToState(Lifecycle.State.DESTROYED)
        verify(mockScanFlow).cancelFlow()
    }

    @Test
    fun `when shouldStartFromBack don't reset upload state`() {
        launchTestFragment(shouldStartFromBack = true)
        verify(mockIdentityViewModel, times(0)).resetDocumentUploadedState()
    }

    @Test
    fun `when not shouldStartFromBack reset upload state`() {
        launchTestFragment(shouldStartFromBack = false)
        verify(mockIdentityViewModel).resetDocumentUploadedState()
    }

    private fun launchTestFragment(shouldStartFromBack: Boolean = false) =
        launchFragmentInContainer(
            bundleOf(IdentityDocumentScanFragment.ARG_SHOULD_START_FROM_BACK to shouldStartFromBack),
            themeResId = R.style.Theme_MaterialComponents
        ) {
            TestFragment(
                viewModelFactoryFor(mockIdentityScanViewModel),
                viewModelFactoryFor(mockIdentityViewModel),
                mockCameraView
            )
        }
}
