package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.IdScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.CameraViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IDScanFragmentTest {
    // Ensure livedata works properly
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = MutableLiveData<IDDetectorAggregator.FinalResult>()
    private val displayStateChanged = MutableLiveData<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockCameraPermissionEnsurable = mock<CameraPermissionEnsureable>()
    private val mockCameraViewModel = mock<CameraViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

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

    private val cameraViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return mockCameraViewModel as T
        }
    }

    @Test
    fun `when created camera permission is requested`() {
        launchIDScanFragment(mockCameraPermissionEnsurable)

        verify(mockCameraPermissionEnsurable).ensureCameraPermission(any(), any())
    }

    @Test
    fun `when camera permission granted cameraAdapter is bound and identityScanFlow is started`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            testCameraPermissionEnsureable.onCameraReady()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(it.requireContext()),
                any(),
                any(),
                same(it.viewLifecycleOwner),
                same(it.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_FRONT)
            )
        }
    }

    @Test
    fun `when front is scanned clicking button triggers back scan`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            testCameraPermissionEnsureable.onCameraReady()
            // mock success of front scan
            finalResultLiveData.postValue(mock())

            // stopScanning() is called
            verify(mockScanFlow).resetFlow()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()

            // mock viewModel target change
            whenever(mockCameraViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_FRONT)

            // button clicked
            IdScanFragmentBinding.bind(it.requireView()).kontinue.callOnClick()

            // verify start to scan back
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(it.requireContext()),
                any(),
                any(),
                same(it.viewLifecycleOwner),
                same(it.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_BACK)
            )
        }
    }

    @Test
    fun `when both sides are scanned clicking button triggers navigation`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.IDScanFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            // scan front
            testCameraPermissionEnsureable.onCameraReady()

            // mock success of front scan
            finalResultLiveData.postValue(mock())

            // mock viewModel target change
            whenever(mockCameraViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_FRONT)

            // click continue, scan back
            val binding = IdScanFragmentBinding.bind(it.requireView())
            binding.kontinue.callOnClick()

            // mock success of back scan
            finalResultLiveData.postValue(mock())

            // mock viewModel target change
            whenever(mockCameraViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_BACK)

            // click continue, navigates
            binding.kontinue.callOnClick()
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.confirmationFragment)
        }
    }

    @Test
    fun `when camera permission denied navigates to permission denied`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(R.navigation.identity_nav_graph)

            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            testCameraPermissionEnsureable.onUserDeniedCameraPermission()

            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()
            assertThat(navController.currentDestination?.id).isEqualTo(R.id.cameraPermissionDeniedFragment)
        }
    }

    @Test
    fun `when final result is received scanFlow is reset and cameraAdapter is unbound`() {
        launchIDScanFragment(testCameraPermissionEnsureable).onFragment {
            testCameraPermissionEnsureable.onCameraReady()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isTrue()

            finalResultLiveData.postValue(mock())

            verify(mockScanFlow).resetFlow()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for ID_FRONT`() {
        whenever(mockCameraViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.front_of_id)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for ID_BACK`() {
        whenever(mockCameraViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_BACK)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.back_of_id)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_back)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Found UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Found>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.hold_still)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated for ID_FRONT`() {
        whenever(mockCameraViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated for ID_BACK`() {
        whenever(mockCameraViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_BACK)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_back)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Satisfied UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Satisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.scanned)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Finished UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Finished>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isTrue()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.scanned)
            )
        }
    }

    private fun launchIDScanFragment(
        cameraPermissionEnsureable: CameraPermissionEnsureable
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        IDScanFragment(
            cameraPermissionEnsureable,
            cameraViewModelFactory
        )
    }

    private fun postDisplayStateChangedDataAndVerifyUI(
        newScanState: IdentityScanState,
        check: (binding: IdScanFragmentBinding, context: Context) -> Unit
    ) {
        launchIDScanFragment(
            mockCameraPermissionEnsurable
        ).onFragment {
            displayStateChanged.postValue((newScanState to mock()))
            check(IdScanFragmentBinding.bind(it.requireView()), it.requireContext())
        }
    }
}
