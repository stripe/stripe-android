package com.stripe.android.identity.navigation

import android.view.View
import androidx.annotation.StringRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.DocSelectionFragmentBinding
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.DRIVING_LICENSE_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.ID_CARD_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.PASSPORT_KEY
import com.stripe.android.identity.networking.Resource
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
            assertThat(binding.passport.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.passportSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.dl.text).isEqualTo(DRIVING_LICENSE_BUTTON_TEXT)
            assertThat(binding.dl.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.dlSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.id.text).isEqualTo(ID_BUTTON_TEXT)
            assertThat(binding.id.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.idSeparator.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `zero choice UI is correctly bound with values locally`() {
        launchDocSelectionFragment { binding, _, docSelectFragment ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_ZERO_CHOICE
            )
            setUpSuccessVerificationPage()

            assertThat(binding.title.text).isEqualTo(DOCUMENT_SELECT_TITLE)
            assertThat(binding.multiSelectionContent.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.singleSelectionContent.visibility).isEqualTo(View.GONE)

            assertThat(binding.passport.text).isEqualTo(
                docSelectFragment.getString(R.string.passport)
            )
            assertThat(binding.passport.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.passportSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.dl.text).isEqualTo(
                docSelectFragment.getString(R.string.driver_license)
            )
            assertThat(binding.dl.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.dlSeparator.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.id.text).isEqualTo(
                docSelectFragment.getString(R.string.id_card)
            )
            assertThat(binding.id.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.idSeparator.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `Passport single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_PASSPORT,
            R.string.single_selection_body_content_passport
        )
    }

    @Test
    fun `ID single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_ID,
            R.string.single_selection_body_content_id
        )
    }

    @Test
    fun `Driver license single choice UI is correctly bound`() {
        verifySingleChoiceUI(
            DOC_SELECT_SINGLE_CHOICE_DL,
            R.string.single_selection_body_content_dl
        )
    }

    @Test
    fun `when scan is available, clicking continue navigates to scan`() {
        launchDocSelectionFragment { binding, navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock scan is available
            // TODO(ccen) add camera permission check later
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(
                    Resource.success(
                        mock()
                    )
                )
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.driverLicenseScanFragment)
        }
    }

    @Test
    fun `when scan is unavailable, clicking continue navigates to upload when requireLiveCapture is false`() {
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
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock scan is not available
            // TODO(ccen) add camera permission check later
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.error())
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.callOnClick()
            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.driverLicenseUploadFragment)
        }
    }

    @Test
    fun `when scan is unavailable, clicking continue navigates to error when requireLiveCapture is true`() {
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
                whenever(mockIdentityViewModel.postVerificationPageData(any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock scan is not available
            // TODO(ccen) add camera permission check later
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.error())
            )
            setUpSuccessVerificationPage()
            binding.singleSelectionContinue.callOnClick()
            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    private fun verifySingleChoiceUI(
        docSelect: VerificationPageStaticContentDocumentSelectPage,
        @StringRes expectedBodyStringRes: Int
    ) {
        launchDocSelectionFragment { binding, _, docSelectionFragment ->
            whenever(verificationPage.documentSelect).thenReturn(
                docSelect
            )
            setUpSuccessVerificationPage()

            assertThat(binding.title.text).isEqualTo(DOCUMENT_SELECT_TITLE)
            assertThat(binding.multiSelectionContent.visibility).isEqualTo(View.GONE)
            assertThat(binding.singleSelectionContent.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.singleSelectionBody.text).isEqualTo(
                docSelectionFragment.getString(expectedBodyStringRes)
            )

            assertThat(binding.singleSelectionContinue.text).isEqualTo(DOCUMENT_SELECT_BUTTON_TEXT)
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
        DocSelectionFragment(viewModelFactoryFor(mockIdentityViewModel))
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

        val DOC_SELECT_MULTI_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BUTTON_TEXT,
                ID_CARD_KEY to ID_BUTTON_TEXT,
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BUTTON_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_PASSPORT = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BUTTON_TEXT,
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_ID = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                ID_CARD_KEY to ID_BUTTON_TEXT,
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_DL = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BUTTON_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT
        )

        val DOC_SELECT_ZERO_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = emptyMap(),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT
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
