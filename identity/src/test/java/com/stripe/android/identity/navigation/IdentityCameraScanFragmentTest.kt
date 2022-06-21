package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.analytics.AnalyticsState
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityCameraScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()

    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
        whenever(it.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()
    private val mockIdentityAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.pageAndModelFiles).thenReturn(mockPageAndModel)
        whenever(it.identityAnalyticsRequestFactory).thenReturn(mockIdentityAnalyticsRequestFactory)
    }

    @Test
    fun `when document front finished result is posted send analytics`() {
        launchTestFragmentWithFinalResult(FINISHED_RESULT_ID_FRONT) {
            val updateBlockCaptor: KArgumentCaptor<(AnalyticsState) -> AnalyticsState> =
                argumentCaptor()
            verify(mockIdentityViewModel).updateAnalyticsState(
                updateBlockCaptor.capture()
            )
            val newState = updateBlockCaptor.firstValue(AnalyticsState())

            assertThat(newState.docFrontModelScore).isEqualTo(DOC_FRONT_SCORE)
        }
    }

    @Test
    fun `when document back finished result is posted send analytics`() {
        launchTestFragmentWithFinalResult(FINISHED_RESULT_ID_BACK) {
            val updateBlockCaptor: KArgumentCaptor<(AnalyticsState) -> AnalyticsState> =
                argumentCaptor()
            verify(mockIdentityViewModel).updateAnalyticsState(
                updateBlockCaptor.capture()
            )
            val newState = updateBlockCaptor.firstValue(AnalyticsState())

            assertThat(newState.docBackModelScore).isEqualTo(DOC_BACK_SCORE)
        }
    }

    @Test
    fun `when selfie finished result is posted send analytics`() {
        launchTestFragmentWithFinalResult(FINISHED_RESULT_SELFIE) {
            val updateBlockCaptor: KArgumentCaptor<(AnalyticsState) -> AnalyticsState> =
                argumentCaptor()
            verify(mockIdentityViewModel).updateAnalyticsState(
                updateBlockCaptor.capture()
            )
            val newState = updateBlockCaptor.firstValue(AnalyticsState())

            assertThat(newState.selfieModelScore).isEqualTo(SELFIE_SCORE)
        }
    }

    @Test
    fun `when document front timeout result is posted send analytics`() {
        launchTestFragmentWithFinalResult(TIMEOUT_RESULT_ID_FRONT) {
            verify(mockIdentityAnalyticsRequestFactory).documentTimeout(
                scanType = eq(IdentityScanState.ScanType.ID_FRONT)
            )

            verify(mockIdentityViewModel).sendAnalyticsRequest(anyOrNull())
        }
    }

    @Test
    fun `when document back timeout result is posted send analytics`() {
        launchTestFragmentWithFinalResult(TIMEOUT_RESULT_ID_BACK) {
            verify(mockIdentityAnalyticsRequestFactory).documentTimeout(
                scanType = eq(IdentityScanState.ScanType.ID_BACK)
            )

            verify(mockIdentityViewModel).sendAnalyticsRequest(anyOrNull())
        }
    }

    @Test
    fun `when selfie timeout result is posted send analytics`() {
        launchTestFragmentWithFinalResult(TIMEOUT_RESULT_SELFIE) {
            verify(mockIdentityAnalyticsRequestFactory).selfieTimeout()

            verify(mockIdentityViewModel).sendAnalyticsRequest(anyOrNull())
        }
    }

    private fun launchTestFragmentWithFinalResult(
        finalResult: IdentityAggregator.FinalResult,
        testBlock: () -> Unit
    ) =
        launchFragmentInContainer(
            themeResId = R.style.Theme_MaterialComponents
        ) {
            TestFragment(
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
            navController.setCurrentDestination(R.id.IDScanFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            finalResultLiveData.postValue(
                finalResult
            )

            val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
            verify(mockIdentityViewModel).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )
            successCaptor.firstValue(SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE)

            testBlock()
        }

    internal class TestFragment(
        identityScanViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory
    ) : IdentityCameraScanFragment(
        identityScanViewModelFactory,
        identityViewModelFactory
    ) {
        override val fragmentId = 0

        override fun onCameraReady() {}

        override fun createCameraAdapter() = mock<Camera1Adapter>()

        override fun updateUI(identityScanState: IdentityScanState) {
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return View(ApplicationProvider.getApplicationContext())
        }

        override fun resetUI() {
        }
    }

    private companion object {
        const val DOC_FRONT_SCORE = 0.12f
        const val DOC_BACK_SCORE = 0.23f
        const val SELFIE_SCORE = 0.34f
        val FINISHED_RESULT_ID_FRONT = IdentityAggregator.FinalResult(
            frame = mock(),
            result = IDDetectorOutput(
                boundingBox = mock(),
                category = Category.ID_FRONT,
                resultScore = DOC_FRONT_SCORE,
                allScores = mock()
            ),
            identityState = IdentityScanState.Finished(
                type = IdentityScanState.ScanType.ID_FRONT,
                transitioner = mock()
            )
        )

        val FINISHED_RESULT_ID_BACK = IdentityAggregator.FinalResult(
            frame = mock(),
            result = IDDetectorOutput(
                boundingBox = mock(),
                category = Category.ID_BACK,
                resultScore = DOC_BACK_SCORE,
                allScores = mock()
            ),
            identityState = IdentityScanState.Finished(
                type = IdentityScanState.ScanType.ID_BACK,
                transitioner = mock()
            )
        )

        val FINISHED_RESULT_SELFIE = IdentityAggregator.FinalResult(
            frame = mock(),
            result = FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = SELFIE_SCORE
            ),
            identityState = IdentityScanState.Finished(
                type = IdentityScanState.ScanType.SELFIE,
                transitioner = mock()
            )
        )

        val TIMEOUT_RESULT_ID_FRONT = IdentityAggregator.FinalResult(
            frame = mock(),
            result = IDDetectorOutput(
                boundingBox = mock(),
                category = Category.ID_FRONT,
                resultScore = DOC_FRONT_SCORE,
                allScores = mock()
            ),
            identityState = IdentityScanState.TimeOut(
                type = IdentityScanState.ScanType.ID_FRONT,
                transitioner = mock()
            )
        )

        val TIMEOUT_RESULT_ID_BACK = IdentityAggregator.FinalResult(
            frame = mock(),
            result = IDDetectorOutput(
                boundingBox = mock(),
                category = Category.ID_BACK,
                resultScore = DOC_BACK_SCORE,
                allScores = mock()
            ),
            identityState = IdentityScanState.TimeOut(
                type = IdentityScanState.ScanType.ID_BACK,
                transitioner = mock()
            )
        )

        val TIMEOUT_RESULT_SELFIE = IdentityAggregator.FinalResult(
            frame = mock(),
            result = FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = SELFIE_SCORE
            ),
            identityState = IdentityScanState.TimeOut(
                type = IdentityScanState.ScanType.SELFIE,
                transitioner = mock()
            )
        )
    }
}
