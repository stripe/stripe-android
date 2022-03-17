package com.stripe.android.identity.navigation

import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.core.model.InternalStripeFilePurpose
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_FAILURE_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.PassportUploadFragmentBinding
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.utils.ARG_SHOULD_SHOW_CAMERA
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.PassportUploadViewModel
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
class PassportUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val uploaded =
        MutableLiveData<Resource<Pair<InternalStripeFile, UploadMethod>>>()
    private val mockUri = mock<Uri>()

    private val mockPassportUploadViewModel = mock<PassportUploadViewModel>().also {
        whenever(it.uploaded).thenReturn(uploaded)
    }

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.documentCapture).thenReturn(DOCUMENT_CAPTURE)
    }

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        whenever(it.observeForVerificationPage(any(), successCaptor.capture(), any())).then {
            successCaptor.firstValue(verificationPage)
        }
    }

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment { binding, _, fragment ->
            verify(mockPassportUploadViewModel).registerActivityResultCaller(same(fragment))

            assertThat(binding.select.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `when shouldShowCamera is true UI is correct`() {
        launchFragment(shouldShowCamera = true) { binding, _, _ ->
            binding.select.callOnClick()
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(BottomSheetDialog::class.java)

            // assert dialog content
            assertThat(dialog.findViewById<Button>(R.id.choose_file).visibility).isEqualTo(View.VISIBLE)
            assertThat(dialog.findViewById<Button>(R.id.take_photo).visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `when shouldShowCamera is false UI is correct`() {
        launchFragment(shouldShowCamera = false) { binding, _, _ ->
            binding.select.callOnClick()
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(BottomSheetDialog::class.java)

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
            uploaded.postValue(Resource.error())

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `verify when kontinue is clicked navigates to confirmation`() {
        launchFragment { binding, navController, _ ->
            runBlocking {
                uploaded.postValue(
                    Resource.success(
                        Pair(
                            InternalStripeFile(id = FILE_ID),
                            UploadMethod.FILEUPLOAD
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
                                highResImage = FILE_ID,
                                uploadMethod = UploadMethod.FILEUPLOAD
                            ),
                            type = IdDocumentParam.Type.PASSPORT
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

    private fun verifyFlow(isTakePhoto: Boolean) {
        launchFragment { binding, _, fragment ->
            binding.select.callOnClick()

            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(BottomSheetDialog::class.java)

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
                verify(mockPassportUploadViewModel).takePhoto(
                    same(fragment.requireContext()),
                    callbackCaptor.capture()
                )
            } else {
                verify(mockPassportUploadViewModel).chooseImage(callbackCaptor.capture())
            }
            uploaded.postValue(Resource.loading())

            // mock photo taken/image chosen
            callbackCaptor.firstValue(mockUri)

            // viewmodel triggers and UI updates
            verify(mockPassportUploadViewModel).uploadImage(
                same(mockUri),
                same(fragment.requireContext()),
                same(DOCUMENT_CAPTURE),
                if (isTakePhoto)
                    eq(UploadMethod.MANUALCAPTURE)
                else
                    eq(UploadMethod.FILEUPLOAD)
            )
            assertThat(binding.select.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.GONE)

            // mock file uploaded
            uploaded.postValue(Resource.success(mock()))

            assertThat(binding.select.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isTrue()
        }
    }

    private fun launchFragment(
        shouldShowCamera: Boolean = true,
        testBlock: (
            binding: PassportUploadFragmentBinding,
            navController: TestNavHostController,
            fragment: PassportUploadFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        fragmentArgs = bundleOf(
            ARG_SHOULD_SHOW_CAMERA to shouldShowCamera
        ),
        themeResId = R.style.Theme_MaterialComponents
    ) {
        PassportUploadFragment(
            viewModelFactoryFor(mockPassportUploadViewModel),
            viewModelFactoryFor(mockIdentityViewModel)
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.passportUploadFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        testBlock(PassportUploadFragmentBinding.bind(it.requireView()), navController, it)
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
                requireLiveCapture = false
            )

        val FILE_ID = "file_id"
    }
}
