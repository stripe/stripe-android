package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.R
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_PASSPORT
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.databinding.IdentityUploadFragmentBinding
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PassportUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()
    private val mockUri = mock<Uri>()

    private val mockIdentityUploadViewModel = mock<IdentityUploadViewModel>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }

    private val documentFrontUploadState = MutableStateFlow(SingleSideDocumentUploadState())
    private val documentBackUploadState = MutableStateFlow(SingleSideDocumentUploadState())

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
    }

    private val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    )

    private val frontPhotoTakenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
    private val backPhotoTakenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
    private val frontImageChosenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
    private val backImageChosenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment { binding, _, fragment ->
            val callbackCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            verify(mockIdentityUploadViewModel).registerActivityResultCaller(
                same(fragment),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture()
            )

            runBlocking {
                verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_FILE_UPLOAD_PASSPORT))
            }

            assertThat(binding.selectFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.separator.visibility).isEqualTo(View.GONE)
            assertThat(binding.backUpload.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `when shouldShowTakePhoto is true UI is correct`() {
        launchFragment(shouldShowTakePhoto = true) { binding, _, _ ->
            binding.selectFront.callOnClick()
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

            // assert dialog content
            assertThat(dialog.findViewById<Button>(R.id.choose_file).visibility).isEqualTo(View.VISIBLE)
            assertThat(dialog.findViewById<Button>(R.id.take_photo).visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `when shouldShowTakePhoto is false UI is correct`() {
        launchFragment(shouldShowTakePhoto = false) { binding, _, _ ->
            binding.selectFront.callOnClick()
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

            // assert dialog content
            assertThat(dialog.findViewById<Button>(R.id.choose_file).visibility).isEqualTo(View.VISIBLE)
            assertThat(dialog.findViewById<Button>(R.id.take_photo).visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `upload front - kontinue enabled`() {
        launchFragment { binding, _, fragment ->
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    VERIFICATION_PAGE_DATA_NOT_MISSING_BACK
                )

                verify(mockIdentityUploadViewModel).registerActivityResultCaller(
                    same(fragment),
                    frontPhotoTakenCaptor.capture(),
                    backPhotoTakenCaptor.capture(),
                    frontImageChosenCaptor.capture(),
                    backImageChosenCaptor.capture()
                )
                takePhotoAndVerify(binding, fragment)

                // mock front uploaded
                documentFrontUploadState.update {
                    SingleSideDocumentUploadState(
                        highResResult = Resource.success(
                            FRONT_HIGH_RES_RESULT
                        )
                    )
                }

                // Front UI done
                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)

                // kontinue button enabled
                assertThat(binding.kontinue.findViewById<Button>(R.id.button).isEnabled).isTrue()
                assertThat(binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.GONE
                )
            }
        }
    }

    private fun takePhotoAndVerify(
        binding: IdentityUploadFragmentBinding,
        fragment: IdentityUploadFragment
    ) {
        binding.selectFront.callOnClick()
        val dialog = ShadowDialog.getLatestDialog()

        // dialog shows up
        assertThat(dialog.isShowing).isTrue()
        assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

        // verify front scan UI
        assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
            fragment.getString(
                R.string.upload_dialog_title_passport
            )
        )

        // click take photo button
        dialog.findViewById<Button>(R.id.take_photo).callOnClick()

        // dialog dismissed
        assertThat(dialog.isShowing).isFalse()

        // mock photo taken
        verify(mockIdentityUploadViewModel).takePhotoFront(
            same(fragment.requireContext())
        )
        frontPhotoTakenCaptor.firstValue(mockUri)

        // verify upload
        verify(mockIdentityViewModel).uploadManualResult(
            uri = same(mockUri),
            isFront = eq(true),
            docCapturePage = same(DOCUMENT_CAPTURE),
            uploadMethod = eq(UploadMethod.MANUALCAPTURE),
            scanType = eq(
                IdentityScanState.ScanType.PASSPORT
            )
        )

        // verify UI update
        assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
        assertThat(binding.progressCircularFront.visibility).isEqualTo(View.VISIBLE)
        assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
    }

    private fun launchFragment(
        shouldShowTakePhoto: Boolean = true,
        testBlock: (
            binding: IdentityUploadFragmentBinding,
            navController: TestNavHostController,
            fragment: PassportUploadFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        fragmentArgs = bundleOf(
            ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
            ARG_SHOULD_SHOW_CHOOSE_PHOTO to true
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestPassportUploadFragment(
            mock(),
            viewModelFactoryFor(mockIdentityViewModel),
            navController
        ).also {
            it.identityUploadViewModelFactory = viewModelFactoryFor(mockIdentityUploadViewModel)
        }
    }.onFragment {
        testBlock(IdentityUploadFragmentBinding.bind(it.requireView()), navController, it)
    }

    internal class TestPassportUploadFragment(
        identityIO: IdentityIO,
        identityViewModelFactory: ViewModelProvider.Factory,
        val navController: TestNavHostController
    ) : PassportUploadFragment(
        identityIO,
        identityViewModelFactory
    ) {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.passportUploadFragment)
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

        private const val FILE_ID = "file_id"

        val FRONT_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(id = FILE_ID),
            scores = null,
            uploadMethod = UploadMethod.FILEUPLOAD
        )
    }
}
