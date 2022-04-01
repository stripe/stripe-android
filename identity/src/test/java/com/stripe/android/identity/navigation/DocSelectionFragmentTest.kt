package com.stripe.android.identity.navigation

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.DocSelectionFragmentBinding
import com.stripe.android.identity.navigation.CameraPermissionDeniedFragment.Companion.ARG_SCAN_TYPE
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.DRIVING_LICENSE_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.ID_CARD_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.PASSPORT_KEY
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentSelectPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DocSelectionFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val verificationPage = mock<VerificationPage>()
    private val mockIdentityViewModel = mock<IdentityViewModel>()
    private val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
    private val onCameraReadyCaptor = argumentCaptor<() -> Unit>()
    private val onUserDeniedCameraPermissionCaptor = argumentCaptor<() -> Unit>()

    private fun setUpErrorVerificationPage() {
        val failureCaptor: KArgumentCaptor<(Throwable?) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel
        ).observeForVerificationPage(
            any(),
            any(),
            failureCaptor.capture()
        )
        failureCaptor.firstValue(null)
    }

    private fun setUpSuccessVerificationPage(times: Int = 1) {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel,
            times(times)
        ).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )
        successCaptor.lastValue(verificationPage)
    }

    @Test
    fun `errorVerificationPage navigates to errorFragment`() {
        launchDocSelectionFragment { _, navController, _ ->
            setUpErrorVerificationPage()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `multi choice UI is correctly bound with values from response`() {
        launchDocSelectionFragment { binding, _, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_MULTI_CHOICE
            )
            setUpSuccessVerificationPage()

            assertThat(binding.title.text).isEqualTo(DOCUMENT_SELECT_TITLE)
            assertThat(binding.multiSelectionContent.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.singleSelectionContent.visibility).isEqualTo(View.GONE)

            assertThat(binding.passport.text).isEqualTo(PASSPORT_BUTTON_TEXT)
            assertThat(binding.passportContainer.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.passportSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.dl.text).isEqualTo(DRIVING_LICENSE_BUTTON_TEXT)
            assertThat(binding.dlContainer.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.dlSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.id.text).isEqualTo(ID_BUTTON_TEXT)
            assertThat(binding.idContainer.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.idSeparator.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `zero choice navigates to error`() {
        launchDocSelectionFragment { _, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_ZERO_CHOICE
            )
            setUpSuccessVerificationPage()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `Passport single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_PASSPORT,
            PASSPORT_SINGLE_BODY_TEXT
        )
    }

    @Test
    fun `ID single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_ID,
            ID_SINGLE_BODY_TEXT
        )
    }

    @Test
    fun `Driver license single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_DL,
            DRIVING_LICENSE_SINGLE_BODY_TEXT
        )
    }

    @Test
    fun `when scan is available, clicking continue navigates to scan`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(
                    Resource.success(
                        mock()
                    )
                )
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission granted
            onCameraReadyCaptor.firstValue()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.driverLicenseScanFragment)
        }
    }

    @Test
    fun `when modelFile is unavailable and camera permission granted, clicking continue navigates to upload when requireLiveCapture is false`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(false)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is not available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.error())
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission granted
            onCameraReadyCaptor.firstValue()

            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.driverLicenseUploadFragment)
        }
    }

    @Test
    fun `when modelFile is unavailable and camera permission granted, clicking continue navigates to error when requireLiveCapture is true`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(true)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock scan is not available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.error())
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission granted
            onCameraReadyCaptor.firstValue()

            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `when camera permission is denied, clicking continue navigates to CameraPermissionDeniedFragment when requireLiveCapture is false`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(false)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.success(mock()))
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission denied
            onUserDeniedCameraPermissionCaptor.firstValue()

            setUpSuccessVerificationPage(2)

            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                [ARG_SCAN_TYPE]
            ).isEqualTo(IdDocumentParam.Type.DRIVINGLICENSE)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.cameraPermissionDeniedFragment)
        }
    }

    @Test
    fun `when camera permission is denied, clicking continue navigates to ErrorFragment when requireLiveCapture is true`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(true)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.success(mock()))
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission denied
            onUserDeniedCameraPermissionCaptor.firstValue()

            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    private fun verifySingleChoiceUI(
        docSelect: VerificationPageStaticContentDocumentSelectPage,
        expectedBodyString: String
    ) {
        launchDocSelectionFragment { binding, _, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                docSelect
            )
            setUpSuccessVerificationPage()

            assertThat(binding.title.text).isEqualTo(DOCUMENT_SELECT_TITLE)
            assertThat(binding.multiSelectionContent.visibility).isEqualTo(View.GONE)
            assertThat(binding.singleSelectionContent.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.singleSelectionBody.text).isEqualTo(
                expectedBodyString
            )

            assertThat(
                binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).text
            ).isEqualTo(
                DOCUMENT_SELECT_BUTTON_TEXT
            )
        }
    }

    private fun launchDocSelectionFragment(
        testBlock: (
            binding: DocSelectionFragmentBinding,
            navController: TestNavHostController,
            docSelectionFragment: DocSelectionFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        DocSelectionFragment(
            viewModelFactoryFor(mockIdentityViewModel),
            mockCameraPermissionEnsureable
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.docSelectionFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        testBlock(DocSelectionFragmentBinding.bind(it.requireView()), navController, it)
    }

    private companion object {
        const val DOCUMENT_SELECT_TITLE = "title"
        const val DOCUMENT_SELECT_BUTTON_TEXT = "button text"
        const val PASSPORT_BUTTON_TEXT = "Passport"
        const val ID_BUTTON_TEXT = "ID"
        const val DRIVING_LICENSE_BUTTON_TEXT = "Driver's license"
        const val PASSPORT_BODY_TEXT = "Passport body"
        const val ID_BODY_TEXT = "ID body"
        const val DRIVING_LICENSE_BODY_TEXT = "Driver's license body"
        const val PASSPORT_SINGLE_BODY_TEXT = "Passport single selection body"
        const val ID_SINGLE_BODY_TEXT = "ID single selection body"
        const val DRIVING_LICENSE_SINGLE_BODY_TEXT = "Driver's license single selection body"

        val DOC_SELECT_MULTI_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BUTTON_TEXT,
                ID_CARD_KEY to ID_BUTTON_TEXT,
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BUTTON_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = null
        )

        val DOC_SELECT_SINGLE_CHOICE_PASSPORT = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BODY_TEXT,
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = PASSPORT_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_ID = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                ID_CARD_KEY to ID_BODY_TEXT,
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = ID_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_DL = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BODY_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = DRIVING_LICENSE_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_ZERO_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = emptyMap(),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = null
        )

        val MISSING_BACK_VERIFICATION_PAGE_DATA = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = emptyList(),
                missing = listOf(VerificationPageDataRequirements.Missing.IDDOCUMENTBACK)
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )
    }
}
