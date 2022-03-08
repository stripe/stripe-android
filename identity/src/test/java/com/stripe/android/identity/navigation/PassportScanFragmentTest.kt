package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.PassportScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Before
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PassportScanFragmentTest {
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
    private val idDetectorModelFile = MutableLiveData<File>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.idDetectorModelFile).thenReturn(idDetectorModelFile)
        whenever(it.idDetectorModelError).thenReturn(mock())
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

    @Before
    fun simulateModelDownloaded() {
        idDetectorModelFile.postValue(mock())
    }

    @Test
    fun `when created camera permission is requested`() {
        launchPassportScanFragment(mockCameraPermissionEnsurable)

        verify(mockCameraPermissionEnsurable).ensureCameraPermission(any(), any())
    }

    @Test
    fun `when camera permission granted cameraAdapter is bound and identityScanFlow is started`() {
        launchPassportScanFragment(testCameraPermissionEnsureable).onFragment {
            testCameraPermissionEnsureable.onCameraReady()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(it.requireContext()),
                any(),
                any(),
                same(it.viewLifecycleOwner),
                same(it.lifecycleScope),
                eq(IdentityScanState.ScanType.PASSPORT)
            )
        }
    }

    @Test
    fun `when scanned clicking button triggers navigation`() {
        launchPassportScanFragment(testCameraPermissionEnsureable).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.passportScanFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            // start scan
            testCameraPermissionEnsureable.onCameraReady()

            // mock success of scan
            finalResultLiveData.postValue(mock())

            // click continue, trigger navigation
            val binding = PassportScanFragmentBinding.bind(it.requireView())
            binding.kontinue.callOnClick()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.confirmationFragment)
        }
    }

    @Test
    fun `when camera permission denied navigates to permission denied`() {
        launchPassportScanFragment(testCameraPermissionEnsureable).onFragment {
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
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.cameraPermissionDeniedFragment)
            assertThat(
                navController.backStack.last()
                    .arguments!![CameraPermissionDeniedFragment.ARG_SCAN_TYPE]
            ).isEqualTo(
                IdentityScanState.ScanType.PASSPORT
            )
        }
    }

    @Test
    fun `when final result is received scanFlow is reset and cameraAdapter is unbound`() {
        launchPassportScanFragment(testCameraPermissionEnsureable).onFragment {
            testCameraPermissionEnsureable.onCameraReady()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isTrue()

            finalResultLiveData.postValue(mock())

            verify(mockScanFlow).resetFlow()
            assertThat(it.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.passport)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_passport)
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
    fun `when displayStateChanged to Unsatisfied UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_passport)
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

    private fun launchPassportScanFragment(
        cameraPermissionEnsureable: CameraPermissionEnsureable
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        PassportScanFragment(
            cameraPermissionEnsureable,
            viewModelFactoryFor(mockCameraViewModel),
            viewModelFactoryFor(mockIdentityViewModel)
        )
    }

    private fun postDisplayStateChangedDataAndVerifyUI(
        newScanState: IdentityScanState,
        check: (binding: PassportScanFragmentBinding, context: Context) -> Unit
    ) {
        launchPassportScanFragment(
            mockCameraPermissionEnsurable
        ).onFragment {
            displayStateChanged.postValue((newScanState to mock()))
            check(PassportScanFragmentBinding.bind(it.requireView()), it.requireContext())
        }
    }
}
