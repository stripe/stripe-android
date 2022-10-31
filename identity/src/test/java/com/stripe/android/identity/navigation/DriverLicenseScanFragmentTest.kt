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
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_BACK
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
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
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DriverLicenseScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.interimResults).thenReturn(mock())
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()

    private val documentFrontUploadState = MutableStateFlow(SingleSideDocumentUploadState())
    private val documentBackUploadState = MutableStateFlow(SingleSideDocumentUploadState())
    private val mockScreenTracker = mock<ScreenTracker>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn mockPageAndModel
        on { documentFrontUploadedState } doReturn documentFrontUploadState
        on { documentBackUploadedState } doReturn documentBackUploadState
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        on { it.fpsTracker } doReturn mock()
        on { it.screenTracker } doReturn mockScreenTracker
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
    }

    private val frontUploadedState = SingleSideDocumentUploadState(
        highResResult = Resource.success(FRONT_HIGH_RES_RESULT),
        lowResResult = Resource.success(FRONT_LOW_RES_RESULT)
    )

    private val backUploadedState = SingleSideDocumentUploadState(
        highResResult = Resource.success(BACK_HIGH_RES_RESULT),
        lowResResult = Resource.success(BACK_LOW_RES_RESULT)
    )

    private val verificationPageRequireSelfie = mock<VerificationPage> {
        on { selfieCapture } doReturn mock()
    }

    private val verificationPageNotRequireSelfie = mock<VerificationPage>()

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
            runBlocking {
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

                documentFrontUploadState.update { frontUploadedState }
                // post returns not missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_BACK
                )

                // button clicked
                IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView()).kontinue
                    .findViewById<Button>(R.id.button).callOnClick()

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(verificationPageNotRequireSelfie)

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
    }

    @Test
    fun `when started with startFromBack with true, scanning with DL_BACK`() {
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
    fun `front scanned and uploaded - response is missing back - back UI is on - back scanned and uploaded - not require selfie - submit`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            runBlocking {
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

                // click continue, try post
                val binding =
                    IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView())

                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                // continue button is triggered to loading
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_FRONT
                    )
                )

                // scan back
                // continue button is reset
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.GONE
                )

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
                    time = 3,
                    targetType = IdentityScanState.ScanType.DL_BACK
                )

                // click button, verify post
                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()
                documentBackUploadState.update { backUploadedState }

                verify(mockIdentityViewModel, times(4)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(verificationPageNotRequireSelfie)

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_TO_CONFIRM
                    )
                )

                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `front scanned and uploaded - response is missing back - back UI is on - back scanned and uploaded - require selfie - to selfie`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            runBlocking {
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

                // click continue, try post
                val binding =
                    IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView())

                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                // continue button is triggered to loading
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_FRONT
                    )
                )

                // scan back
                // continue button is reset
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.GONE
                )

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
                    time = 3,
                    targetType = IdentityScanState.ScanType.DL_BACK
                )

                // click button, verify post
                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()
                documentBackUploadState.update { backUploadedState }

                verify(mockIdentityViewModel, times(4)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(verificationPageRequireSelfie)

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_TO_SELFIE
                    )
                )

                assertThat(navController.currentDestination?.id).isEqualTo(R.id.selfieFragment)
            }
        }
    }

    @Test
    fun `front scanned uploaded - response is not missing back - not require selfie - submit`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            runBlocking {
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

                // click continue, try post
                val binding =
                    IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView())

                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                // continue button is triggered to loading
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns not missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(verificationPageNotRequireSelfie)

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_FRONT
                    )
                )

                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `front scanned uploaded - response is not missing back - require selfie - to selfie`() {
        launchDriverLicenseFragment().onFragment { driverLicenseScanFragment ->
            runBlocking {
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

                // click continue, try post
                val binding =
                    IdentityDocumentScanFragmentBinding.bind(driverLicenseScanFragment.requireView())

                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                // continue button is triggered to loading
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns not missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(verificationPageRequireSelfie)

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.DRIVINGLICENSE,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_FRONT_SELFIE
                    )
                )

                assertThat(navController.currentDestination?.id).isEqualTo(R.id.selfieFragment)
            }
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
            assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                View.GONE
            )
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
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
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isFalse()
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
