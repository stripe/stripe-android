package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.DRIVER_LICENSE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCAN_TYPE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.IdentityDocumentScanFragmentBinding
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DriverLicenseScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()

    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.interimResults).thenReturn(mock())
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()

    private val documentUploadState =
        MutableStateFlow(DocumentUploadState())
    private val mockScreenTracker = mock<ScreenTracker>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn mockPageAndModel
        on { documentUploadState } doReturn documentUploadState
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        on { it.fpsTracker } doReturn mock()
        on { it.screenTracker } doReturn mockScreenTracker
    }

    private val errorDocumentUploadState = mock<DocumentUploadState> {
        on { hasError() } doReturn true
    }

    private val anyLoadingDocumentUploadState = mock<DocumentUploadState> {
        on { isAnyLoading() } doReturn true
    }

    private val bothUploadedDocumentUploadState = DocumentUploadState(
        frontHighResResult = Resource.success(FRONT_HIGH_RES_RESULT),
        frontLowResResult = Resource.success(FRONT_LOW_RES_RESULT),
        backHighResResult = Resource.success(BACK_HIGH_RES_RESULT),
        backLowResResult = Resource.success(BACK_LOW_RES_RESULT)
    )

    @Before
    fun simulateModelDownloaded() {
        mockPageAndModel.postValue(
            Resource.success(
                IdentityViewModel.PageAndModelFiles(
                    SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE,
                    mock(),
                    mock()
                )
            )
        )
    }

    @Test
    fun `when started analytics event is sent`() {
        launchDriverLicenseFragment().onFragment {
            runBlocking {
                mockScreenTracker.screenTransitionFinish(eq(SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE))
            }
            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCAN_TYPE] == DRIVER_LICENSE
                }
            )
        }
    }

    @Test
    fun `when front is scanned file is uploaded and clicking button triggers back scan`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            // verify start to scan front
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(driverLicenseScanFragment.requireContext()),
                any(),
                any(),
                same(driverLicenseScanFragment.viewLifecycleOwner),
                same(driverLicenseScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.DL_FRONT)
            )

            // mock success of front scan
            val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            finalResultLiveData.postValue(mockFrontFinalResult)
            whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.DL_FRONT)
            verifyUploadedWithFinalResult(
                mockFrontFinalResult,
                targetType = IdentityScanState.ScanType.DL_FRONT
            )

            // stopScanning() is called
            verify(mockScanFlow).resetFlow()
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isFalse()

            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.DL_FRONT)

            // button clicked
            IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView()).kontinue
                .findViewById<Button>(R.id.button).callOnClick()

            // verify start to scan back
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(driverLicenseScanFragment.requireContext()),
                any(),
                any(),
                same(driverLicenseScanFragment.viewLifecycleOwner),
                same(driverLicenseScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.DL_BACK)
            )
        }
    }

    @Test
    fun `when started with startFromBack with true, scanning with ID_BACK`() {
        launchDriverLicenseFragment(shouldStartFromBack = true).onFragment { driverLicenseScanFragment ->
            // verify start to scan back
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(driverLicenseScanFragment.requireContext()),
                any(),
                any(),
                same(driverLicenseScanFragment.viewLifecycleOwner),
                same(driverLicenseScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.DL_BACK)
            )
        }
    }

    @Test
    fun `when both sides are scanned and files uploaded succeeded and no selfie required, clicking button triggers post`() {
        simulateBothSidesScanned { _, _ ->
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )
                // mock bothUploaded success
                documentUploadState.update {
                    bothUploadedDocumentUploadState
                }

                // mock identityViewModel.observeForVerificationPage - already called 2 times
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                // verify navigation attempts
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_TO_CONFIRM
                    )
                )

                // no selfie required, send postVerificationPageSubmit
                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `when both sides are scanned and files uploaded succeeded and selfie is required, clicking button navigates to selfie`() {
        simulateBothSidesScanned { navController, _ ->
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )
                // mock bothUploaded success
                documentUploadState.update {
                    bothUploadedDocumentUploadState
                }

                // mock identityViewModel.observeForVerificationPage - already called 2 times
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                val mockVerificationPage = mock<VerificationPage> {
                    on { selfieCapture } doReturn mock() // return non null selfieCapture
                }
                successCaptor.lastValue.invoke(mockVerificationPage)

                verify(mockScreenTracker).screenTransitionStart(
                    eq(
                        SCREEN_NAME_LIVE_CAPTURE_DRIVER_LICENSE
                    ),
                    any()
                )

                // verify navigation attempts
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_TO_SELFIE
                    )
                )

                // selfie required, navigates to selfie
                assertThat(navController.currentDestination?.id).isEqualTo(R.id.selfieFragment)
            }
        }
    }

    @Test
    fun `when both sides are scanned but files uploaded failed, clicking button navigate to error`() {
        simulateBothSidesScanned { navController, _ ->
            // mock bothUploaded error
            documentUploadState.update {
                errorDocumentUploadState
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `when both sides are scanned and files are being uploaded, clicking button toggles loading state`() {
        simulateBothSidesScanned { _, binding ->
            // mock bothUploaded loading
            documentUploadState.update {
                anyLoadingDocumentUploadState
            }

            assertThat(
                binding.kontinue.findViewById<MaterialButton>(R.id.button).isEnabled
            ).isFalse()
            assertThat(
                binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility
            ).isEqualTo(
                View.VISIBLE
            )
        }
    }

    @Test
    fun `when final result is received scanFlow is reset and cameraAdapter is unbound`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()

            finalResultLiveData.postValue(
                mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
            )

            verify(mockScanFlow).resetFlow()
            assertThat(driverLicenseScanFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for DL_FRONT`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.DL_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.front_of_dl)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_dl_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for DL_BACK`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.DL_BACK)
        postDisplayStateChangedDataAndVerifyUI(
            mock<IdentityScanState.Initial>(),
            shouldStartFromBack = true
        ) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.back_of_dl)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_dl_back)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Found UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Found>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.hold_still)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated for DL_FRONT`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.DL_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.front_of_dl)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_dl_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated for DL_BACK`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.DL_BACK)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.back_of_dl)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_dl_back)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Satisfied UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Satisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.VISIBLE)
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
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility)
                .isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility)
                .isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility)
                .isEqualTo(View.INVISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isTrue()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.scanned)
            )
        }
    }

    private fun verifyUploadedWithFinalResult(
        finalResult: IdentityAggregator.FinalResult,
        time: Int = 1,
        targetType: IdentityScanState.ScanType
    ) {
        val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
        verify(mockIdentityViewModel, times(time)).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )

        val mockVerificationPage = mock<VerificationPage>()
        successCaptor.lastValue.invoke(mockVerificationPage)
        verify(mockIdentityViewModel).uploadScanResult(
            same(finalResult),
            same(mockVerificationPage),
            eq(targetType)
        )
    }

    private fun simulateBothSidesScanned(afterScannedBlock: (TestNavHostController, IdentityDocumentScanFragmentBinding) -> Unit) {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.driverLicenseScanFragment)
            Navigation.setViewNavController(
                driverLicenseScanFragment.requireView(),
                navController
            )
            // scan front
            // mock success of front scan
            val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.DL_FRONT)
            finalResultLiveData.postValue(mockFrontFinalResult)
            verifyUploadedWithFinalResult(
                mockFrontFinalResult,
                targetType = IdentityScanState.ScanType.DL_FRONT
            )

            // click continue, scan back
            val binding =
                IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView())
            binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

            // mock success of back scan
            val mockBackFinalResult = mock<IdentityAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.DL_BACK)
            finalResultLiveData.postValue(mockBackFinalResult)
            verifyUploadedWithFinalResult(
                mockBackFinalResult,
                2,
                targetType = IdentityScanState.ScanType.DL_BACK
            )

            // click continue, navigates
            binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

            afterScannedBlock(navController, binding)
        }
    }

    private fun launchDriverLicenseFragment(shouldStartFromBack: Boolean = false) =
        launchFragmentInContainer(
            bundleOf(IdentityDocumentScanFragment.ARG_SHOULD_START_FROM_BACK to shouldStartFromBack),
            themeResId = R.style.Theme_MaterialComponents
        ) {
            DriverLicenseScanFragment(
                viewModelFactoryFor(mockIdentityScanViewModel),
                viewModelFactoryFor(mockIdentityViewModel)
            )
        }

    private fun postDisplayStateChangedDataAndVerifyUI(
        newScanState: IdentityScanState,
        shouldStartFromBack: Boolean = false,
        check: (binding: IdentityDocumentScanFragmentBinding, context: Context) -> Unit
    ) {
        launchDriverLicenseFragment(shouldStartFromBack).onFragment {
            displayStateChanged.postValue((newScanState to mock()))
            check(IdentityDocumentScanFragmentBinding.bind(it.requireView()), it.requireContext())
        }
    }

    private companion object {
        val FRONT_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "frontHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val FRONT_LOW_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "frontLowResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val BACK_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "backHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val BACK_LOW_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "frontHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
    }
}
