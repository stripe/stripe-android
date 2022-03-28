package com.stripe.android.identity.navigation

import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.core.model.InternalStripeFilePurpose
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.FrontBackUploadFragmentBinding
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CAMERA
import com.stripe.android.identity.utils.PairMediatorLiveData
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel.UploadedResult
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
class FrontBackUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val frontHighResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val backHighResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val highResUploaded = PairMediatorLiveData(frontHighResUploaded, backHighResUploaded)
    private val mockUri = mock<Uri>()
    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.firstValue(verificationPage)
        }
        whenever(it.frontHighResUploaded).thenReturn(frontHighResUploaded)
        whenever(it.backHighResUploaded).thenReturn(backHighResUploaded)
        whenever(it.highResUploaded).thenReturn(highResUploaded)
    }

    private val mockFrontBackUploadViewModel = mock<FrontBackUploadViewModel>()

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment { binding, _, fragment ->
            verify(mockFrontBackUploadViewModel).registerActivityResultCaller(same(fragment))

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
    fun `when shouldShowCamera is true UI is correct`() {
        launchFragment(shouldShowCamera = true) { binding, _, _ ->
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
    fun `when shouldShowCamera is false UI is correct`() {
        launchFragment(shouldShowCamera = false) { binding, _, _ ->
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
            frontHighResUploaded.postValue(Resource.error())

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify back upload failure navigates to error fragment `() {
        launchFragment { _, navController, _ ->
            backHighResUploaded.postValue(Resource.error())

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify uploadFinished updates UI`() {
        launchFragment { binding, _, _ ->
            frontHighResUploaded.postValue(
                Resource.success(
                    UploadedResult(
                        uploadedStripeFile = InternalStripeFile(id = FRONT_UPLOADED_ID),
                        scores = null,
                        uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                    )
                )
            )
            backHighResUploaded.postValue(
                Resource.success(
                    UploadedResult(
                        uploadedStripeFile = InternalStripeFile(id = BACK_UPLOADED_ID),
                        scores = null,
                        uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                    )
                )
            )

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
    fun `verify when kontinue is clicked and post succeeds navigates to confirmation`() {
        launchFragment { binding, navController, _ ->
            runBlocking {
                frontHighResUploaded.postValue(
                    Resource.success(
                        UploadedResult(
                            uploadedStripeFile = InternalStripeFile(id = FRONT_UPLOADED_ID),
                            scores = null,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        )
                    )
                )
                backHighResUploaded.postValue(
                    Resource.success(
                        UploadedResult(
                            uploadedStripeFile = InternalStripeFile(id = BACK_UPLOADED_ID),
                            scores = null,
                            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                        )
                    )
                )

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

                assertThat(collectedDataParamCaptor.firstValue).isEqualTo(
                    CollectedDataParam(
                        idDocument = IdDocumentParam(
                            front = DocumentUploadParam(
                                highResImage = FRONT_UPLOADED_ID,
                                uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                            ),
                            back = DocumentUploadParam(
                                highResImage = BACK_UPLOADED_ID,
                                uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD
                            ),
                            type = IdDocumentParam.Type.IDCARD
                        )
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
    fun `verify when kontinue is clicked and data is null navigates to error`() {
        launchFragment { binding, navController, _ ->
            // leave frontBackPair.data null
            highResUploaded.postValue(Resource.success(mock()))

            binding.kontinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    private fun verifyFlow(scanType: IdentityScanState.ScanType, isTakePhoto: Boolean) {
        launchFragment { binding, _, fragment ->
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

            // viewmodel triggers
            val callbackCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()

            if (isTakePhoto) {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).takePhotoFront(
                        same(fragment.requireContext()),
                        callbackCaptor.capture()
                    )
                    frontHighResUploaded.postValue(Resource.loading())
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).takePhotoBack(
                        same(fragment.requireContext()),
                        callbackCaptor.capture()
                    )
                    backHighResUploaded.postValue(Resource.loading())
                }
            } else {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).chooseImageFront(callbackCaptor.capture())
                    frontHighResUploaded.postValue(Resource.loading())
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).chooseImageBack(callbackCaptor.capture())
                    backHighResUploaded.postValue(Resource.loading())
                }
            }

            // mock photo taken/image chosen
            callbackCaptor.firstValue(mockUri)

            // viewmodel triggers and UI updates
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                verify(mockIdentityViewModel).uploadManualResult(
                    uri = same(mockUri),
                    isFront = eq(true),
                    docCapturePage = same(DOCUMENT_CAPTURE),
                    uploadMethod =
                    if (isTakePhoto)
                        eq(DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    else
                        eq(DocumentUploadParam.UploadMethod.FILEUPLOAD)
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
                    if (isTakePhoto)
                        eq(DocumentUploadParam.UploadMethod.MANUALCAPTURE)
                    else
                        eq(DocumentUploadParam.UploadMethod.FILEUPLOAD)
                )
                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            }

            // mock file uploaded
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                frontHighResUploaded.postValue(Resource.success(mock()))

                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                backHighResUploaded.postValue(Resource.success(mock()))

                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    private fun launchFragment(
        shouldShowCamera: Boolean = true,
        testBlock: (
            binding: FrontBackUploadFragmentBinding,
            navController: TestNavHostController,
            fragment: FrontBackUploadFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        fragmentArgs = bundleOf(
            ARG_SHOULD_SHOW_CAMERA to shouldShowCamera
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestFragment(
            viewModelFactoryFor(mockFrontBackUploadViewModel),
            viewModelFactoryFor(mockIdentityViewModel)
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.IDUploadFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        testBlock(FrontBackUploadFragmentBinding.bind(it.requireView()), navController, it)
    }

    internal class TestFragment(
        frontBackUploadViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory
    ) :
        FrontBackUploadFragment(frontBackUploadViewModelFactory, identityViewModelFactory) {
        override val titleRes = R.string.file_upload
        override val contextRes = R.string.file_upload_content_id
        override val frontTextRes = R.string.front_of_id
        override val backTextRes = R.string.back_of_id
        override val frontCheckMarkContentDescription = R.string.front_of_id_selected
        override val backCheckMarkContentDescription = R.string.back_of_id_selected
        override val frontScanType = IdentityScanState.ScanType.ID_FRONT
        override val backScanType = IdentityScanState.ScanType.ID_BACK
    }

    private companion object {
        val DOCUMENT_CAPTURE =
            VerificationPageStaticContentDocumentCapturePage(
                autocaptureTimeout = 0,
                filePurpose = InternalStripeFilePurpose.IdentityPrivate.code,
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
    }
}
