package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCAN_TYPE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_ID
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class IdentityUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    private val documentFrontUploadState = MutableStateFlow(SingleSideDocumentUploadState())
    private val documentBackUploadState = MutableStateFlow(SingleSideDocumentUploadState())

    private val errorDocumentUploadState = mock<SingleSideDocumentUploadState> {
        on { hasError() } doReturn true
        on { getError() } doReturn mock()
    }

    private val collectedData = MutableStateFlow(CollectedDataParam())
    private val frontCollectedInfo =
        MutableStateFlow(SingleSideDocumentUploadState() to CollectedDataParam())
    private val backCollectedInfo =
        MutableStateFlow(SingleSideDocumentUploadState() to CollectedDataParam())
    private val missings = MutableStateFlow(listOf<Requirement>())

    private val mockScreenTracker = mock<ScreenTracker>()

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.lastValue(verificationPage)
        }

        whenever(it.documentFrontUploadedState).thenReturn(documentFrontUploadState)
        whenever(it.documentBackUploadedState).thenReturn(documentBackUploadState)

        whenever(it.identityAnalyticsRequestFactory).thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            ).also {
                it.verificationPage = mock()
            }
        )
        whenever(it.screenTracker).thenReturn(mockScreenTracker)
        whenever(it.uiContext).thenReturn(testDispatcher)
        whenever(it.workContext).thenReturn(testDispatcher)
        whenever(it.frontCollectedInfo).thenReturn(frontCollectedInfo)
        whenever(it.backCollectedInfo).thenReturn(backCollectedInfo)
        whenever(it.collectedData).thenReturn(collectedData)
        whenever(it.missingRequirements).thenReturn(missings)
        whenever(it.errorCause).thenReturn(mock())
    }

    private val mockFrontBackUploadViewModel = mock<IdentityUploadViewModel>()

    private val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    ).also {
        it.setGraph(
            R.navigation.identity_nav_graph
        )
        it.setCurrentDestination(R.id.IDUploadFragment)
    }

    @Test
    fun `when initialized viewmodel registers activityResultCaller`() {
        launchFragment { _, fragment ->
            val callbackCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            verify(mockFrontBackUploadViewModel).registerActivityResultCaller(
                same(fragment),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture()
            )
        }
    }

    @Test
    fun `verify front upload failure navigates to error fragment `() {
        launchFragment { navController, _ ->
            frontCollectedInfo.update {
                errorDocumentUploadState to CollectedDataParam()
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify back upload failure navigates to error fragment `() {
        launchFragment { navController, _ ->
            backCollectedInfo.update {
                errorDocumentUploadState to CollectedDataParam()
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    private fun launchFragment(
        shouldShowTakePhoto: Boolean = true,
        shouldShowChoosePhoto: Boolean = true,
        testBlock: (
            navController: TestNavHostController,
            fragment: IdentityUploadFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        fragmentArgs = bundleOf(
            ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
            ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestFragment(
            mock(),
            viewModelFactoryFor(mockIdentityViewModel),
            navController
        ).also {
            it.identityUploadViewModelFactory = viewModelFactoryFor(mockFrontBackUploadViewModel)
        }
    }.onFragment {
        runBlocking {
            verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_FILE_UPLOAD_ID))
        }
        verify(mockIdentityViewModel).sendAnalyticsRequest(
            argThat {
                eventName == EVENT_SCREEN_PRESENTED &&
                    (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_FILE_UPLOAD_ID &&
                    (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCAN_TYPE] == ID // from frontScanType = IdentityScanState.ScanType.ID_FRONT
            }
        )
        testBlock(navController, it)
    }

    internal class TestFragment(
        identityIO: IdentityIO,
        identityViewModelFactory: ViewModelProvider.Factory,
        val navController: TestNavHostController
    ) :
        IdentityUploadFragment(identityIO, identityViewModelFactory) {
        override val titleRes = R.string.file_upload
        override val contextRes = R.string.file_upload_content_id
        override val frontTextRes = R.string.front_of_id
        override var backTextRes: Int? = R.string.back_of_id
        override val frontCheckMarkContentDescription = R.string.front_of_id_selected
        override var backCheckMarkContentDescription: Int? = R.string.back_of_id_selected
        override val frontScanType = IdentityScanState.ScanType.ID_FRONT
        override var backScanType: IdentityScanState.ScanType? = IdentityScanState.ScanType.ID_BACK
        override val collectedDataParamType = CollectedDataParam.Type.IDCARD
        override val fragmentId = R.id.IDUploadFragment
        override val route = IDUploadDestination.ROUTE.route

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): ComposeView {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            Navigation.setViewNavController(
                view,
                navController
            )
            return view
        }
    }

    private companion object {
        val DOCUMENT_CAPTURE =
            VerificationPageStaticContentDocumentCapturePage(
                autocaptureTimeout = 0,
                filePurpose = StripeFilePurpose.IdentityPrivate.code,
                highResImageCompressionQuality = 0.9f,
                highResImageCropPadding = 0f,
                highResImageMaxDimension = 512,
                lowResImageCompressionQuality = 0f,
                lowResImageMaxDimension = 0,
                models = mock(),
                requireLiveCapture = false,
                motionBlurMinDuration = 500,
                motionBlurMinIou = 0.95f
            )
    }
}
