package com.stripe.android.identity.navigation

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SelfieUploadState
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.FaceDetectorTransitioner
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class SelfieFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val interimResultLiveData = MutableLiveData<IdentityAggregator.InterimResult>()
    private val displayStateChangedFlow =
        MutableStateFlow<Pair<IdentityScanState, IdentityScanState?>?>(null)
    private val mockScanFlow = mock<IdentityScanFlow>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { it.identityScanFlow } doReturn mockScanFlow
        on { it.finalResult } doReturn finalResultLiveData
        on { it.interimResults } doReturn interimResultLiveData
        on { it.displayStateChangedFlow } doReturn displayStateChangedFlow
        on { it.cameraAdapterInitialized } doReturn mock()
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()

    private val selfieUploadState = MutableStateFlow(SelfieUploadState())
    private val mockFPSTracker = mock<FPSTracker>()

    private val errorUploadState = mock<SelfieUploadState> {
        on { hasError() } doReturn true
    }

    private val loadingUploadState = mock<SelfieUploadState> {
        on { hasError() } doReturn false
        on { isAnyLoading() } doReturn true
    }

    private val mockUploadedResult = mock<UploadedResult> {
        on { uploadedStripeFile }.thenReturn(StripeFile(id = "testId"))
    }
    private val successUploadState = SelfieUploadState(
        firstHighResResult = Resource.success(mockUploadedResult),
        firstLowResResult = Resource.success(mockUploadedResult),
        lastHighResResult = Resource.success(mockUploadedResult),
        lastLowResResult = Resource.success(mockUploadedResult),
        bestHighResResult = Resource.success(mockUploadedResult),
        bestLowResResult = Resource.success(mockUploadedResult)
    )

    private val mockScreenTracker = mock<ScreenTracker>()

    private val verificationPageLiveData = MutableLiveData<Resource<VerificationPage>>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn mockPageAndModel
        on { selfieUploadState } doReturn selfieUploadState
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            ).also {
                it.verificationPage = mock()
            }
        on { fpsTracker } doReturn mockFPSTracker
        on { screenTracker } doReturn mockScreenTracker
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
        on { verificationPage } doReturn verificationPageLiveData
    }

    @Before
    fun mockSuccessVerificationPage() {
        verificationPageLiveData.postValue(
            Resource.success(
                SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
            )
        )
    }

    @Test
    fun `when selfieUploadState all uploaded, clicking continue triggers navigation`() {
        launchSelfieFragment { _, selfieFragment ->
            runBlocking {
                displayStateChangedFlow.update { (FINISHED to mock()) }

                selfieUploadState.update {
                    successUploadState
                }

                interimResultLiveData.postValue(
                    IdentityAggregator.InterimResult(
                        FINISHED
                    )
                )
                finalResultLiveData.postValue(
                    IdentityAggregator.FinalResult(
                        mock(),
                        mock(),
                        FINISHED
                    )
                )
                // mock button click by calling collectUploadedStateAndUploadForCollectedSelfies
                val trainingConsent = false
                selfieFragment.collectUploadedStateAndUploadForCollectedSelfies(trainingConsent)

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createForSelfie(
                            firstHighResResult = requireNotNull(successUploadState.firstHighResResult.data),
                            firstLowResResult = requireNotNull(successUploadState.firstLowResResult.data),
                            lastHighResResult = requireNotNull(successUploadState.lastHighResResult.data),
                            lastLowResResult = requireNotNull(successUploadState.lastLowResResult.data),
                            bestHighResResult = requireNotNull(successUploadState.bestHighResResult.data),
                            bestLowResResult = requireNotNull(successUploadState.bestLowResResult.data),
                            trainingConsent = trainingConsent,
                            bestFaceScore = BEST_FACE_SCORE,
                            faceScoreVariance = SCORE_VARIANCE,
                            numFrames = NUM_FRAMES
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `when selfieUploadState hasError, clicking continue navigates to ErrorFragment`() {
        launchSelfieFragment { navController, selfieFragment ->
            runBlocking {
                selfieUploadState.update {
                    errorUploadState
                }

                // mock button click by calling collectUploadedStateAndUploadForCollectedSelfies
                selfieFragment.collectUploadedStateAndUploadForCollectedSelfies(false)

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `when selfieUploadState isLoading, clicking continue stays on SelfieFragment`() {
        launchSelfieFragment { navController, selfieFragment ->
            runBlocking {
                displayStateChangedFlow.update { (FINISHED to mock()) }
                selfieUploadState.update {
                    loadingUploadState
                } // mock button click by calling collectUploadedStateAndUploadForCollectedSelfies
                selfieFragment.collectUploadedStateAndUploadForCollectedSelfies(false)

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.selfieFragment)
            }
        }
    }

    private fun launchSelfieFragment(
        testBlock: (TestNavHostController, SelfieFragment) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        SelfieFragment(
            viewModelFactoryFor(mockIdentityScanViewModel),
            viewModelFactoryFor(mockIdentityViewModel)
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.selfieFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        testBlock(navController, it)
    }

    private companion object {
        val dummyBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val FILTERED_FRAMES = listOf(
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = dummyBitmap,
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.81f
            ), // first
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = dummyBitmap,
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.9f
            ), // best
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = dummyBitmap,
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.82f
            ) // last
        )

        const val SCORE_VARIANCE = 0.1f
        const val BEST_FACE_SCORE = 0.91f
        const val NUM_FRAMES = 8

        val FINISHED = IdentityScanState.Finished(
            type = IdentityScanState.ScanType.SELFIE,
            transitioner = mock<FaceDetectorTransitioner> {
                on { filteredFrames }.thenReturn(FILTERED_FRAMES)
                on { scoreVariance }.thenReturn(SCORE_VARIANCE)
                on { bestFaceScore }.thenReturn(BEST_FACE_SCORE)
                on { numFrames }.thenReturn(NUM_FRAMES)
            }
        )
    }
}
