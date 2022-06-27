package com.stripe.android.identity.navigation

import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.SelfieScanFragmentBinding
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SelfieUploadState
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SelfieFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()

    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { it.identityScanFlow } doReturn mockScanFlow
        on { it.finalResult } doReturn finalResultLiveData
        on { it.interimResults } doReturn mock()
        on { it.displayStateChanged } doReturn displayStateChanged
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()
    private val documentUploadState =
        MutableStateFlow(DocumentUploadState())

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

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn mockPageAndModel
        on { documentUploadState } doReturn documentUploadState
        on { selfieUploadState } doReturn selfieUploadState
        on { identityAnalyticsRequestFactory } doReturn
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        on { fpsTracker } doReturn mockFPSTracker
    }

    @Test
    fun `when initialized UI is reset and bound`() {
        launchSelfieFragment { binding, _, _ ->
            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_SELFIE
                }
            )

            val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
            verify(
                mockIdentityViewModel,
                times(1)
            ).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            val mockSelfieCapture = mock<VerificationPageStaticContentSelfieCapturePage> {
                on { consentText } doReturn CONSENT_TEXT
            }
            successCaptor.lastValue(
                mock {
                    on { selfieCapture } doReturn mockSelfieCapture
                }
            )

            verify(mockIdentityViewModel).resetSelfieUploadedState()
            assertThat(binding.scanningView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.resultView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
            assertThat(binding.allowImageCollection.text.toString()).isEqualTo(CONSENT_TEXT)
        }
    }

    @Test
    fun `when Found UI is set and flash once`() {
        launchSelfieFragment { binding, _, fragment ->
            assertThat(fragment.flashed).isFalse()

            displayStateChanged.postValue((mock<IdentityScanState.Found>() to mock()))
            assertThat(binding.message.text).isEqualTo(fragment.getText(R.string.capturing))
            assertThat(fragment.flashed).isTrue()

            displayStateChanged.postValue((mock<IdentityScanState.Found>() to mock()))
            assertThat(binding.message.text).isEqualTo(fragment.getText(R.string.capturing))
            assertThat(fragment.flashed).isTrue()
        }
    }

    @Test
    fun `when Satisfied UI is set`() {
        launchSelfieFragment { binding, _, fragment ->
            assertThat(fragment.flashed).isFalse()

            displayStateChanged.postValue((mock<IdentityScanState.Satisfied>() to mock()))
            assertThat(binding.message.text).isEqualTo(fragment.getText(R.string.selfie_capture_complete))
        }
    }

    @Test
    fun `when finished UI is toggled`() {
        launchSelfieFragment { binding, _, fragment ->
            assertThat(fragment.selfieResultAdapter.itemCount).isEqualTo(0)

            displayStateChanged.postValue((FINISHED to mock()))

            assertThat(binding.scanningView.visibility).isEqualTo(View.GONE)
            assertThat(binding.resultView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(true)
            assertThat(fragment.selfieResultAdapter.itemCount).isEqualTo(FILTERED_FRAMES.size)
        }
    }

    @Test
    fun `when selfieUploadState all uploaded, clicking continue triggers navigation`() {
        launchSelfieFragment { binding, _, _ ->
            runBlocking {
                displayStateChanged.postValue((FINISHED to mock()))
                assertThat(binding.kontinue.isEnabled).isEqualTo(true)
                selfieUploadState.update {
                    successUploadState
                }
                finalResultLiveData.postValue(
                    IdentityAggregator.FinalResult(
                        mock(),
                        mock(),
                        FINISHED
                    )
                )
                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createForSelfie(
                            firstHighResResult = requireNotNull(successUploadState.firstHighResResult.data),
                            firstLowResResult = requireNotNull(successUploadState.firstLowResResult.data),
                            lastHighResResult = requireNotNull(successUploadState.lastHighResResult.data),
                            lastLowResResult = requireNotNull(successUploadState.lastLowResResult.data),
                            bestHighResResult = requireNotNull(successUploadState.bestHighResResult.data),
                            bestLowResResult = requireNotNull(successUploadState.bestLowResResult.data),
                            trainingConsent = binding.allowImageCollection.isChecked,
                            bestFaceScore = BEST_FACE_SCORE,
                            faceScoreVariance = SCORE_VARIANCE,
                            numFrames = NUM_FRAMES
                        )
                    ),
                    eq(ClearDataParam.SELFIE_TO_CONFIRM)
                )
            }
        }
    }

    @Test
    fun `when selfieUploadState hasError, clicking continue navigates to ErrorFragment`() {
        launchSelfieFragment { binding, navController, _ ->
            runBlocking {
                displayStateChanged.postValue((FINISHED to mock()))
                assertThat(binding.kontinue.isEnabled).isEqualTo(true)
                selfieUploadState.update {
                    errorUploadState
                }
                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `when selfieUploadState isLoading, clicking continue triggers toggles loading state`() {
        launchSelfieFragment { binding, _, _ ->
            runBlocking {
                displayStateChanged.postValue((FINISHED to mock()))
                assertThat(binding.kontinue.isEnabled).isEqualTo(true)
                selfieUploadState.update {
                    loadingUploadState
                }
                binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

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
    }

    private fun launchSelfieFragment(testBlock: (SelfieScanFragmentBinding, TestNavHostController, SelfieFragment) -> Unit) =
        launchFragmentInContainer(
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
            navController.setCurrentDestination(R.id.couldNotCaptureFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            testBlock(SelfieScanFragmentBinding.bind(it.requireView()), navController, it)
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
        const val CONSENT_TEXT = "TEST CONSENT TEXT"

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
