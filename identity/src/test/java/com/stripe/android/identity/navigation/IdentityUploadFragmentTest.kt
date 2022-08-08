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
import androidx.navigation.NavArgument
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.ID
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCAN_TYPE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_FILE_UPLOAD_ID
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.databinding.IdentityUploadFragmentBinding
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_IS_NAVIGATED_UP_TO
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
import org.robolectric.shadows.ShadowDialog

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IdentityUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockUri = mock<Uri>()
    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    private val verificationPageWithSelfie = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
        whenever(it.selfieCapture).thenReturn(mock())
    }

    private val documentUploadState =
        MutableStateFlow(DocumentUploadState())

    private val errorDocumentUploadState = mock<DocumentUploadState> {
        on { hasError() } doReturn true
        on { getError() } doReturn mock()
    }

    private val mockScreenTracker = mock<ScreenTracker>()

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.firstValue(verificationPage)
        }
        whenever(it.documentUploadState).thenReturn(documentUploadState)
        whenever(it.identityAnalyticsRequestFactory).thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        )
        whenever(it.screenTracker).thenReturn(mockScreenTracker)
        whenever(it.uiContext).thenReturn(testDispatcher)
        whenever(it.workContext).thenReturn(testDispatcher)
    }

    private val mockIdentityViewModelWithSelfie = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.firstValue(verificationPageWithSelfie)
        }
        whenever(it.documentUploadState).thenReturn(documentUploadState)
        whenever(it.identityAnalyticsRequestFactory).thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        )
        whenever(it.screenTracker).thenReturn(mockScreenTracker)
        whenever(it.uiContext).thenReturn(testDispatcher)
        whenever(it.workContext).thenReturn(testDispatcher)
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
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment { binding, _, fragment ->
            val callbackCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            verify(mockFrontBackUploadViewModel).registerActivityResultCaller(
                same(fragment),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture(),
                callbackCaptor.capture()
            )

            assertThat(binding.selectFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.selectBack.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)

            assertThat(binding.titleText.text).isEqualTo(fragment.getString(R.string.file_upload))
            assertThat(binding.contentText.text).isEqualTo(fragment.getString(R.string.file_upload_content_id))
            assertThat(binding.labelFront.text).isEqualTo(fragment.getString(R.string.front_of_id))
            assertThat(binding.labelBack.text).isEqualTo(fragment.getString(R.string.back_of_id))
            assertThat(binding.finishedCheckMarkFront.contentDescription).isEqualTo(
                fragment.getString(
                    R.string.front_of_id_selected
                )
            )
            assertThat(binding.finishedCheckMarkBack.contentDescription).isEqualTo(
                fragment.getString(
                    R.string.back_of_id_selected
                )
            )
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
    fun `when shouldShowChoosePhoto is true UI is correct`() {
        launchFragment(shouldShowChoosePhoto = true) { binding, _, _ ->
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
    fun `when shouldShowChoosePhoto is false UI is correct`() {
        launchFragment(shouldShowChoosePhoto = false) { binding, _, _ ->
            binding.selectFront.callOnClick()
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

            // assert dialog content
            assertThat(dialog.findViewById<Button>(R.id.choose_file).visibility).isEqualTo(View.GONE)
            assertThat(dialog.findViewById<Button>(R.id.take_photo).visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `verify select front take photo interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_FRONT, true)
    }

    @Test
    fun `verify select front choose file interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_FRONT, false)
    }

    @Test
    fun `verify select back take photo interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_BACK, true)
    }

    @Test
    fun `verify select back choose file interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_BACK, false)
    }

    @Test
    fun `verify front upload failure navigates to error fragment `() {
        launchFragment { _, navController, _ ->
            documentUploadState.update {
                errorDocumentUploadState
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify back upload failure navigates to error fragment `() {
        launchFragment { _, navController, _ ->
            documentUploadState.update {
                errorDocumentUploadState
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify uploadFinished updates UI`() {
        launchFragment { binding, _, _ ->
            documentUploadState.update {
                DocumentUploadState(
                    frontHighResResult = Resource.success(
                        FRONT_HIGH_RES_RESULT_FILEUPLOAD
                    ),
                    backHighResResult = Resource.success(
                        BACK_HIGH_RES_RESULT_FILEUPLOAD
                    )
                )
            }

            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.kontinue.isEnabled).isTrue()
        }
    }

    @Test
    fun `verify when selfie not required, kontinue is clicked and post succeeds navigates to confirmation`() {
        launchFragment { binding, navController, _ ->
            runBlocking {
                documentUploadState.update {
                    DocumentUploadState(
                        frontHighResResult = Resource.success(
                            FRONT_HIGH_RES_RESULT_FILEUPLOAD
                        ),
                        backHighResResult = Resource.success(
                            BACK_HIGH_RES_RESULT_FILEUPLOAD
                        )
                    )
                }

                val collectedDataParamCaptor: KArgumentCaptor<CollectedDataParam> = argumentCaptor()
                val clearDataParamCaptor: KArgumentCaptor<ClearDataParam> = argumentCaptor()
                whenever(
                    mockIdentityViewModel.postVerificationPageData(
                        collectedDataParamCaptor.capture(),
                        clearDataParamCaptor.capture()
                    )
                ).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )
                whenever(mockIdentityViewModel.postVerificationPageSubmit()).thenReturn(
                    CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
                )

                binding.kontinue.findViewById<MaterialButton>(R.id.button).callOnClick()

                verify(mockScreenTracker).screenTransitionStart(
                    eq(SCREEN_NAME_FILE_UPLOAD_ID),
                    any()
                )

                assertThat(collectedDataParamCaptor.firstValue).isEqualTo(
                    CollectedDataParam(
                        idDocumentFront = DocumentUploadParam(
                            highResImage = FRONT_UPLOADED_ID,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        ),
                        idDocumentBack = DocumentUploadParam(
                            highResImage = BACK_UPLOADED_ID,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        ),
                        idDocumentType = CollectedDataParam.Type.IDCARD
                    )
                )
                assertThat(clearDataParamCaptor.firstValue).isEqualTo(
                    ClearDataParam.UPLOAD_TO_CONFIRM
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.confirmationFragment)
            }
        }
    }

    @Test
    fun `verify when selfie is required, clicking kontinue navigates to selfie`() {
        launchFragment(requireSelfie = true) { binding, navController, _ ->
            runBlocking {
                documentUploadState.update {
                    DocumentUploadState(
                        frontHighResResult = Resource.success(
                            FRONT_HIGH_RES_RESULT_FILEUPLOAD
                        ),
                        backHighResResult = Resource.success(
                            BACK_HIGH_RES_RESULT_FILEUPLOAD
                        )
                    )
                }

                val collectedDataParamCaptor: KArgumentCaptor<CollectedDataParam> = argumentCaptor()
                val clearDataParamCaptor: KArgumentCaptor<ClearDataParam> = argumentCaptor()
                whenever(
                    mockIdentityViewModelWithSelfie.postVerificationPageData(
                        collectedDataParamCaptor.capture(),
                        clearDataParamCaptor.capture()
                    )
                ).thenReturn(
                    CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
                )

                binding.kontinue.findViewById<MaterialButton>(R.id.button).callOnClick()

                verify(mockScreenTracker).screenTransitionStart(
                    eq(SCREEN_NAME_FILE_UPLOAD_ID),
                    any()
                )

                assertThat(collectedDataParamCaptor.firstValue).isEqualTo(
                    CollectedDataParam(
                        idDocumentFront = DocumentUploadParam(
                            highResImage = FRONT_UPLOADED_ID,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        ),
                        idDocumentBack = DocumentUploadParam(
                            highResImage = BACK_UPLOADED_ID,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        ),
                        idDocumentType = CollectedDataParam.Type.IDCARD
                    )
                )
                assertThat(clearDataParamCaptor.firstValue).isEqualTo(
                    ClearDataParam.UPLOAD_TO_SELFIE
                )

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.selfieFragment)
            }
        }
    }

    @Test
    fun `when not navigatedUp and previous backstack entry is couldNotCapture don't reset uploadState`() {
        navController.setCurrentDestination(R.id.couldNotCaptureFragment)
        navController.navigate(R.id.IDUploadFragment)
        launchFragment { _, _, _ ->
            verify(mockIdentityViewModel, times(0)).resetDocumentUploadedState()
        }
    }

    @Test
    fun `when is navigateUp and previous backstack entry is couldNotCapture reset uploadState`() {
        navController.setCurrentDestination(R.id.couldNotCaptureFragment)
        navController.navigate(R.id.IDUploadFragment)
        navController.navigate(R.id.confirmationFragment)

        // Simulate the behavior set in IdentityActivity.onBackPressedCallback
        navController.previousBackStackEntry?.destination?.addArgument(
            ARG_IS_NAVIGATED_UP_TO,
            NavArgument.Builder()
                .setDefaultValue(true)
                .build()
        )

        navController.navigateUp()
        launchFragment { _, _, _ ->
            verify(mockIdentityViewModel).resetDocumentUploadedState()
        }
    }

    @Test
    fun `when previous backstack entry is not couldNotCapture reset uploadState`() {
        navController.setCurrentDestination(R.id.confirmationFragment)
        navController.navigate(R.id.IDUploadFragment)

        launchFragment { _, _, _ ->
            verify(mockIdentityViewModel).resetDocumentUploadedState()
        }
    }

    private fun verifyFlow(scanType: IdentityScanState.ScanType, isTakePhoto: Boolean) {
        launchFragment { binding, _, fragment ->
            val frontPhotoTakenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            val backPhotoTakenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            val frontImageChosenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            val backImageChosenCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()
            verify(mockFrontBackUploadViewModel).registerActivityResultCaller(
                same(fragment),
                frontPhotoTakenCaptor.capture(),
                backPhotoTakenCaptor.capture(),
                frontImageChosenCaptor.capture(),
                backImageChosenCaptor.capture()
            )
            // click select front button
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                binding.selectFront.callOnClick()
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                binding.selectBack.callOnClick()
            }
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

            when (scanType) {
                IdentityScanState.ScanType.ID_FRONT -> {
                    assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                        fragment.getString(
                            R.string.upload_dialog_title_id_front
                        )
                    )
                }
                IdentityScanState.ScanType.ID_BACK -> {
                    assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                        fragment.getString(
                            R.string.upload_dialog_title_id_back
                        )
                    )
                }
                IdentityScanState.ScanType.DL_FRONT -> {
                    assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                        fragment.getString(
                            R.string.upload_dialog_title_dl_front
                        )
                    )
                }
                IdentityScanState.ScanType.DL_BACK -> {
                    assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                        fragment.getString(
                            R.string.upload_dialog_title_dl_back
                        )
                    )
                }
                IdentityScanState.ScanType.PASSPORT -> {
                    assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                        fragment.getString(
                            R.string.upload_dialog_title_passport
                        )
                    )
                }
                else -> {} // no-op
            }

            // click take photo button
            if (isTakePhoto) {
                dialog.findViewById<Button>(R.id.take_photo).callOnClick()
            } else {
                dialog.findViewById<Button>(R.id.choose_file).callOnClick()
            }

            // dialog dismissed
            assertThat(dialog.isShowing).isFalse()

            if (isTakePhoto) {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).takePhotoFront(
                        same(fragment.requireContext())
                    )
                    frontPhotoTakenCaptor.firstValue(mockUri)
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).takePhotoBack(
                        same(fragment.requireContext())
                    )
                    backPhotoTakenCaptor.firstValue(mockUri)
                }
            } else {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).chooseImageFront()
                    frontImageChosenCaptor.firstValue(mockUri)
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).chooseImageBack()
                    backImageChosenCaptor.firstValue(mockUri)
                }
            }

            // viewmodel triggers and UI updates
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                verify(mockIdentityViewModel).uploadManualResult(
                    uri = same(mockUri),
                    isFront = eq(true),
                    docCapturePage = same(DOCUMENT_CAPTURE),
                    uploadMethod =
                    if (isTakePhoto) {
                        eq(DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    } else {
                        eq(DocumentUploadParam.UploadMethod.FILEUPLOAD)
                    },
                    scanType = eq(scanType)
                )
                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                verify(mockIdentityViewModel).uploadManualResult(
                    uri = same(mockUri),
                    isFront = eq(false),
                    docCapturePage = same(DOCUMENT_CAPTURE),
                    uploadMethod =
                    if (isTakePhoto) {
                        eq(DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    } else {
                        eq(DocumentUploadParam.UploadMethod.FILEUPLOAD)
                    },
                    scanType = eq(scanType)
                )
                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            }

            // mock file uploaded
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                documentUploadState.update {
                    DocumentUploadState(
                        frontHighResResult =
                        Resource.success(
                            if (isTakePhoto) FRONT_HIGH_RES_RESULT_MANUALCAPTURE else FRONT_HIGH_RES_RESULT_FILEUPLOAD
                        )
                    )
                }

                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                documentUploadState.update {
                    DocumentUploadState(
                        backHighResResult =
                        Resource.success(
                            if (isTakePhoto) BACK_HIGH_RES_RESULT_MANUALCAPTURE else BACK_HIGH_RES_RESULT_FILEUPLOAD
                        )
                    )
                }

                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    private fun launchFragment(
        shouldShowTakePhoto: Boolean = true,
        shouldShowChoosePhoto: Boolean = true,
        requireSelfie: Boolean = false,
        testBlock: (
            binding: IdentityUploadFragmentBinding,
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
            viewModelFactoryFor(if (requireSelfie) mockIdentityViewModelWithSelfie else mockIdentityViewModel),
            navController
        ).also {
            it.identityUploadViewModelFactory = viewModelFactoryFor(mockFrontBackUploadViewModel)
        }
    }.onFragment {
        (if (requireSelfie) mockIdentityViewModelWithSelfie else mockIdentityViewModel).let { identityViewModel ->
            runBlocking {
                verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_FILE_UPLOAD_ID))
            }
            verify(identityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_FILE_UPLOAD_ID &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCAN_TYPE] == ID // from frontScanType = IdentityScanState.ScanType.ID_FRONT
                }
            )
        }
        testBlock(IdentityUploadFragmentBinding.bind(it.requireView()), navController, it)
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
        override val fragmentId = R.id.IDUploadFragment
        override val presentedId = "TEST_FRAGMENT_PRENTED"

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
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

        const val FRONT_UPLOADED_ID = "id_front"
        const val BACK_UPLOADED_ID = "id_back"

        val FRONT_HIGH_RES_RESULT_FILEUPLOAD = UploadedResult(
            uploadedStripeFile = StripeFile(id = FRONT_UPLOADED_ID),
            scores = null,
            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
        )

        val BACK_HIGH_RES_RESULT_FILEUPLOAD = UploadedResult(
            uploadedStripeFile = StripeFile(id = BACK_UPLOADED_ID),
            scores = null,
            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
        )

        val FRONT_HIGH_RES_RESULT_MANUALCAPTURE = UploadedResult(
            uploadedStripeFile = StripeFile(id = FRONT_UPLOADED_ID),
            scores = null,
            uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE
        )

        val BACK_HIGH_RES_RESULT_MANUALCAPTURE = UploadedResult(
            uploadedStripeFile = StripeFile(id = BACK_UPLOADED_ID),
            scores = null,
            uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE
        )
    }
}
