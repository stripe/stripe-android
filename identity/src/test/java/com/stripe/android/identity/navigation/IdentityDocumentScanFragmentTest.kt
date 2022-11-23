package com.stripe.android.identity.navigation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_BACK
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_SELFIE
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.utils.fragmentIdToScreenName
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
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
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IdentityDocumentScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val interimResultsLiveData = MutableLiveData<IdentityAggregator.InterimResult>()
    private val displayStateChangedFlow =
        MutableStateFlow<Pair<IdentityScanState, IdentityScanState?>?>(null)
    private val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)
    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.interimResults).thenReturn(interimResultsLiveData)
        whenever(it.displayStateChangedFlow).thenReturn(displayStateChangedFlow)
        whenever(it.targetScanTypeFlow).thenReturn(targetScanTypeFlow)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()
    private val documentFrontUploadState = MutableStateFlow(SingleSideDocumentUploadState())
    private val documentBackUploadState = MutableStateFlow(SingleSideDocumentUploadState())

    private val mockScreenTracker = mock<ScreenTracker>()
    private val mockFPSTracker = mock<FPSTracker>()
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { it.pageAndModelFiles } doReturn mockPageAndModel
        on { documentFrontUploadedState } doReturn documentFrontUploadState
        on { documentBackUploadedState } doReturn documentBackUploadState
        on { it.identityAnalyticsRequestFactory } doReturn (
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            ).also {
                it.verificationPage = mock()
            }
            )
        on { it.fpsTracker } doReturn mock()
        on { screenTracker } doReturn mock()
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
        on { it.screenTracker } doReturn mockScreenTracker
        on { it.fpsTracker } doReturn mockFPSTracker
    }

    private val frontUploadedState = SingleSideDocumentUploadState(
        highResResult = Resource.success(FRONT_HIGH_RES_RESULT),
        lowResResult = Resource.success(FRONT_LOW_RES_RESULT)
    )

    private val backUploadedState = SingleSideDocumentUploadState(
        highResResult = Resource.success(BACK_HIGH_RES_RESULT),
        lowResResult = Resource.success(BACK_LOW_RES_RESULT)
    )

    internal class TestFragment(
        identityScanViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory
    ) : IdentityDocumentScanFragment(
        identityScanViewModelFactory,
        identityViewModelFactory
    ) {
        var currentState: IdentityScanState? = null
        var onCameraReadyIsCalled = false
        override val frontScanType = IdentityScanState.ScanType.ID_FRONT
        override val backScanType = IdentityScanState.ScanType.ID_BACK
        override val fragmentId = TEST_FRAGMENT_ID
        override val frontTitleStringRes = R.string.front_of_id
        override val backTitleStringRes = R.string.back_of_id
        override val frontMessageStringRes = R.string.position_id_front
        override val backMessageStringRes = R.string.position_id_back
        override val collectedDataParamType = CollectedDataParam.Type.IDCARD

        override fun onCameraReady() {
            super.onCameraReady()
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
            simulateModelDownloaded()

            assertThat(it.cameraAdapter).isNotNull()
            assertThat(it.onCameraReadyIsCalled).isTrue()
        }
    }

    @Test
    fun `when not require live capture finalResult is posted with Timeout navigates to couldNotCaptureFragment`() {
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
            interimResultsLiveData.postValue(
                IdentityAggregator.InterimResult(
                    mock<IdentityScanState.Finished> {
                        on { it.isFinal } doReturn true
                    }
                )
            )

            val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
            verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            successCaptor.lastValue(SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE)

            verify(mockScanFlow).resetFlow()
            assertThat(testFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.couldNotCaptureFragment)
            assertThat(
                IdentityScanState.ScanType.values()[
                    requireNotNull(navController.backStack.last().arguments)
                        .getInt(
                            CouldNotCaptureFragment.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
                        )
                ]
            ).isEqualTo(IdentityScanState.ScanType.ID_FRONT)
            assertThat(
                requireNotNull(navController.backStack.last().arguments).getBoolean(
                    CouldNotCaptureFragment.ARG_REQUIRE_LIVE_CAPTURE
                )
            ).isEqualTo(false)
        }
    }

    @Test
    fun `when require live capture finalResult is posted with Timeout navigates to couldNotCaptureFragment`() {
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
            interimResultsLiveData.postValue(
                IdentityAggregator.InterimResult(
                    mock<IdentityScanState.Finished> {
                        on { it.isFinal } doReturn true
                    }
                )
            )

            val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
            verify(mockIdentityViewModel, times(2)).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            successCaptor.lastValue(SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE)

            verify(mockScanFlow).resetFlow()
            assertThat(testFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.couldNotCaptureFragment)
            assertThat(
                IdentityScanState.ScanType.values()[
                    requireNotNull(navController.backStack.last().arguments)
                        .getInt(
                            CouldNotCaptureFragment.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
                        )
                ]
            ).isEqualTo(IdentityScanState.ScanType.ID_FRONT)
            assertThat(
                requireNotNull(navController.backStack.last().arguments).getBoolean(
                    CouldNotCaptureFragment.ARG_REQUIRE_LIVE_CAPTURE
                )
            ).isEqualTo(true)
        }
    }

    @Test
    fun `when destroyed scanFlow is cancelled`() {
        launchTestFragment().moveToState(Lifecycle.State.DESTROYED)
        verify(mockScanFlow).cancelFlow()
    }

    @Test
    fun `when not shouldStartFromBack reset upload state and start scan front`() {
        launchTestFragment(shouldStartFromBack = false).onFragment { fragment ->
            simulateModelDownloaded()
            verify(mockIdentityViewModel).resetDocumentUploadedState()

            // verify start to scan back
            assertThat(fragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(fragment.requireContext()),
                any(),
                any(),
                same(fragment.viewLifecycleOwner),
                same(fragment.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_FRONT)
            )
        }
    }

    @Test
    fun `when shouldStartFromBack don't reset upload state and start scan back`() {
        launchTestFragment(shouldStartFromBack = true).onFragment { fragment ->
            simulateModelDownloaded()
            verify(mockIdentityViewModel, times(0)).resetDocumentUploadedState()
            // verify start to scan back
            assertThat(fragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(fragment.requireContext()),
                any(),
                any(),
                same(fragment.viewLifecycleOwner),
                same(fragment.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_BACK)
            )
        }
    }

    @Test
    fun `when started analytics event is sent`() {
        launchTestFragment().onFragment {
            val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
            verify(mockIdentityViewModel).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            successCaptor.lastValue.invoke(SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE)

            runBlocking {
                verify(mockScreenTracker).screenTransitionFinish(eq(TEST_FRAGMENT_ID.fragmentIdToScreenName()))
            }
            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == IdentityAnalyticsRequestFactory.EVENT_SCREEN_PRESENTED &&
                        (params[IdentityAnalyticsRequestFactory.PARAM_EVENT_META_DATA] as Map<*, *>)[IdentityAnalyticsRequestFactory.PARAM_SCREEN_NAME] == IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE_ID &&
                        (params[IdentityAnalyticsRequestFactory.PARAM_EVENT_META_DATA] as Map<*, *>)[IdentityAnalyticsRequestFactory.PARAM_SCAN_TYPE] == IdentityAnalyticsRequestFactory.ID
                }
            )
        }
    }

    @Test
    fun `front scanned and uploaded - response is missing back - back scanned and uploaded - not require selfie - post submit`() {
        launchTestFragment().onFragment { fragment ->
            runBlocking {
                val navController = TestNavHostController(
                    ApplicationProvider.getApplicationContext()
                )
                navController.setGraph(
                    R.navigation.identity_nav_graph
                )
                navController.setCurrentDestination(R.id.IDScanFragment)
                Navigation.setViewNavController(
                    fragment.requireView(),
                    navController
                )
                simulateModelDownloaded()

                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_FRONT)
                )
                // mock success of front scan
                val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_FRONT)
                finalResultLiveData.postValue(mockFrontFinalResult)
                verifyUploadedWithFinalResult(
                    mockFrontFinalResult,
                    time = 2,
                    targetType = IdentityScanState.ScanType.ID_FRONT
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_FRONT.isFront()
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    )
                )
                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_BACK)
                )

                // mock success of back scan
                val mockBackFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_BACK)
                finalResultLiveData.postValue(mockBackFinalResult)
                verifyUploadedWithFinalResult(
                    mockBackFinalResult,
                    time = 4,
                    targetType = IdentityScanState.ScanType.ID_BACK
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_BACK.isFront()
                )
                documentBackUploadState.update { backUploadedState }

                verify(mockIdentityViewModel, times(5)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )

                // post returns not missing selfie
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
                )

                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    )
                )

                // post submit
                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `front scanned and uploaded - response is missing back - back scanned and uploaded - require selfie - to selfie`() {
        launchTestFragment().onFragment { fragment ->
            runBlocking {
                val navController = TestNavHostController(
                    ApplicationProvider.getApplicationContext()
                )
                navController.setGraph(
                    R.navigation.identity_nav_graph
                )
                navController.setCurrentDestination(R.id.IDScanFragment)
                Navigation.setViewNavController(
                    fragment.requireView(),
                    navController
                )
                simulateModelDownloaded()

                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_FRONT)
                )
                // mock success of front scan
                val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_FRONT)
                finalResultLiveData.postValue(mockFrontFinalResult)
                verifyUploadedWithFinalResult(
                    mockFrontFinalResult,
                    time = 2,
                    targetType = IdentityScanState.ScanType.ID_FRONT
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_FRONT.isFront()
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    )
                )
                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_BACK)
                )

                // mock success of back scan
                val mockBackFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_BACK)
                finalResultLiveData.postValue(mockBackFinalResult)
                verifyUploadedWithFinalResult(
                    mockBackFinalResult,
                    time = 4,
                    targetType = IdentityScanState.ScanType.ID_BACK
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_BACK.isFront()
                )
                documentBackUploadState.update { backUploadedState }

                verify(mockIdentityViewModel, times(5)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )

                // post returns not missing selfie
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_SELFIE
                )

                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT
                        )
                    )
                )

                // navigates to selfie
                assertThat(navController.currentDestination?.id).isEqualTo(R.id.selfieFragment)
            }
        }
    }

    @Test
    fun `front scanned and uploaded - response is not back and not require selfie - post submit`() {
        launchTestFragment().onFragment { fragment ->
            runBlocking {
                val navController = TestNavHostController(
                    ApplicationProvider.getApplicationContext()
                )
                navController.setGraph(
                    R.navigation.identity_nav_graph
                )
                navController.setCurrentDestination(R.id.IDScanFragment)
                Navigation.setViewNavController(
                    fragment.requireView(),
                    navController
                )
                simulateModelDownloaded()

                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_FRONT)
                )
                // mock success of front scan
                val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_FRONT)
                finalResultLiveData.postValue(mockFrontFinalResult)
                verifyUploadedWithFinalResult(
                    mockFrontFinalResult,
                    time = 2,
                    targetType = IdentityScanState.ScanType.ID_FRONT
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_FRONT.isFront()
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns not missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    )
                )

                // post submit
                verify(mockIdentityViewModel).postVerificationPageSubmit()
            }
        }
    }

    @Test
    fun `front scanned and uploaded - response is not back and require selfie - to selfie`() {
        launchTestFragment().onFragment { fragment ->
            runBlocking {
                val navController = TestNavHostController(
                    ApplicationProvider.getApplicationContext()
                )
                navController.setGraph(
                    R.navigation.identity_nav_graph
                )
                navController.setCurrentDestination(R.id.IDScanFragment)
                Navigation.setViewNavController(
                    fragment.requireView(),
                    navController
                )
                simulateModelDownloaded()

                // verify start scanning back
                verify(mockScanFlow).startFlow(
                    same(fragment.requireContext()),
                    any(),
                    any(),
                    same(fragment.viewLifecycleOwner),
                    same(fragment.lifecycleScope),
                    eq(IdentityScanState.ScanType.ID_FRONT)
                )
                // mock success of front scan
                val mockFrontFinalResult = mock<IdentityAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
                // mock viewModel target change
                whenever(mockIdentityScanViewModel.targetScanType)
                    .thenReturn(IdentityScanState.ScanType.ID_FRONT)
                finalResultLiveData.postValue(mockFrontFinalResult)
                verifyUploadedWithFinalResult(
                    mockFrontFinalResult,
                    time = 2,
                    targetType = IdentityScanState.ScanType.ID_FRONT
                )

                // mock click continue by calling collectDocumentUploadedStateAndPost
                fragment.collectDocumentUploadedStateAndPost(
                    fragment.collectedDataParamType,
                    IdentityScanState.ScanType.ID_FRONT.isFront()
                )

                documentFrontUploadState.update { frontUploadedState }

                // post returns not missing back
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_MISSING_SELFIE
                )

                // observeForVerificationPage - trigger onSuccess
                val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
                verify(mockIdentityViewModel, times(3)).observeForVerificationPage(
                    any(),
                    successCaptor.capture(),
                    any()
                )
                successCaptor.lastValue.invoke(mock())

                // verify post request
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            type = CollectedDataParam.Type.IDCARD,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    )
                )

                // navigates to selfie
                assertThat(navController.currentDestination?.id).isEqualTo(R.id.selfieFragment)
            }
        }
    }

    private fun simulateModelDownloaded() {
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

    private fun launchTestFragment(shouldStartFromBack: Boolean = false) =
        launchFragmentInContainer(
            bundleOf(IdentityDocumentScanFragment.ARG_SHOULD_START_FROM_BACK to shouldStartFromBack),
            themeResId = R.style.Theme_MaterialComponents
        ) {
            TestFragment(
                viewModelFactoryFor(mockIdentityScanViewModel),
                viewModelFactoryFor(mockIdentityViewModel),
            )
        }

    private companion object {
        val TEST_FRAGMENT_ID = R.id.IDScanFragment
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
