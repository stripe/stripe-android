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
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.IdentityUploadFragmentBinding
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CHOOSE_PHOTO
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_TAKE_PHOTO
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class PassportUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()
    private val mockUri = mock<Uri>()

    private val mockIdentityUploadViewModel = mock<IdentityUploadViewModel>()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }

    private val documentUploadState =
        MutableStateFlow(DocumentUploadState())

    private val errorDocumentUploadState = mock<DocumentUploadState> {
        on { hasError() } doReturn true
    }

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.firstValue(verificationPage)
        }
        whenever(it.documentUploadState).thenReturn(documentUploadState)
    }

    private val navController = TestNavHostController(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment { binding, _, fragment ->
            verify(mockIdentityUploadViewModel).registerActivityResultCaller(same(fragment))

            assertThat(binding.selectFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.separator.visibility).isEqualTo(View.GONE)
            assertThat(binding.backUpload.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
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
    fun `verify take photo interactions`() {
        verifyFlow(true)
    }

    @Test
    fun `verify choose file interactions`() {
        verifyFlow(false)
    }

    @Test
    fun `verify upload failure navigates to error fragment `() {
        launchFragment { _, navController, _ ->
            documentUploadState.update {
                errorDocumentUploadState
            }

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify when kontinue is clicked navigates to confirmation`() {
        launchFragment { binding, navController, _ ->
            runBlocking {
                documentUploadState.update {
                    DocumentUploadState(
                        frontHighResResult = Resource.success(FRONT_HIGH_RES_RESULT)
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

                assertThat(collectedDataParamCaptor.firstValue).isEqualTo(
                    CollectedDataParam(
                        idDocumentFront = DocumentUploadParam(
                            highResImage = FILE_ID,
                            uploadMethod = UploadMethod.FILEUPLOAD
                        ),
                        idDocumentType = CollectedDataParam.Type.PASSPORT
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

    private fun verifyFlow(isTakePhoto: Boolean) {
        launchFragment { binding, _, fragment ->
            binding.selectFront.callOnClick()

            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(AppCompatDialog::class.java)

            assertThat(dialog.findViewById<TextView>(R.id.title).text).isEqualTo(
                fragment.getString(
                    R.string.upload_dialog_title_passport
                )
            )
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
                verify(mockIdentityUploadViewModel).takePhotoFront(
                    same(fragment.requireContext()),
                    callbackCaptor.capture()
                )
            } else {
                verify(mockIdentityUploadViewModel).chooseImageFront(callbackCaptor.capture())
            }

            // mock photo taken/image chosen
            callbackCaptor.firstValue(mockUri)

            // viewmodel triggers and UI updates
            verify(mockIdentityViewModel).uploadManualResult(
                uri = same(mockUri),
                isFront = eq(true),
                docCapturePage = same(DOCUMENT_CAPTURE),
                uploadMethod =
                if (isTakePhoto)
                    eq(UploadMethod.MANUALCAPTURE)
                else
                    eq(UploadMethod.FILEUPLOAD)
            )
            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)

            // mock file uploaded
            documentUploadState.update {
                DocumentUploadState(
                    frontHighResResult = Resource.success(FRONT_HIGH_RES_RESULT)
                )
            }

            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isTrue()
        }
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
            viewModelFactoryFor(mockIdentityUploadViewModel),
            viewModelFactoryFor(mockIdentityViewModel),
            navController
        )
    }.onFragment {
        testBlock(IdentityUploadFragmentBinding.bind(it.requireView()), navController, it)
    }

    internal class TestPassportUploadFragment(
        identityUploadViewModelFactory: ViewModelProvider.Factory,
        identityViewModelFactory: ViewModelProvider.Factory,
        val navController: TestNavHostController
    ) : PassportUploadFragment(
        identityUploadViewModelFactory, identityViewModelFactory
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
