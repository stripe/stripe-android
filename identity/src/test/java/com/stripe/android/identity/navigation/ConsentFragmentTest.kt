package com.stripe.android.identity.navigation

import android.net.Uri
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ConsentFragmentBinding
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirementError
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
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
internal class ConsentFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.biometricConsent).thenReturn(
            VerificationPageStaticContentConsentPage(
                acceptButtonText = CONSENT_ACCEPT_TEXT,
                title = CONSENT_TITLE,
                privacyPolicy = CONSENT_PRIVACY_POLICY,
                timeEstimate = CONSENT_TIME_ESTIMATE,
                body = CONSENT_BODY,
                declineButtonText = CONSENT_DECLINE_TEXT
            )
        )
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    VerificationPageRequirements.Missing.BIOMETRICCONSENT
                )
            )
        )
    }

    private val correctVerificationData = mock<VerificationPageData>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageDataRequirements(
                errors = emptyList(),
                missing = listOf(VerificationPageDataRequirements.Missing.IDDOCUMENTTYPE)
            )
        )
    }

    private val incorrectVerificationData = mock<VerificationPageData>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageDataRequirements(
                errors = listOf(
                    VerificationPageDataRequirementError(
                        body = ERROR_BODY,
                        buttonText = ERROR_BUTTON_TEXT,
                        requirement = VerificationPageDataRequirementError.Requirement.BIOMETRICCONSENT,
                        title = ERROR_TITLE
                    )
                ),
                missing = emptyList()
            )
        )
    }

    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.args).thenReturn(
            IdentityVerificationSheetContract.Args(
                verificationSessionId = VERIFICATION_SESSION_ID,
                ephemeralKeySecret = EPHEMERAL_KEY,
                brandLogo = BRAND_LOGO
            )
        )
    }

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

    private fun setUpSuccessVerificationPage() {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel,
            times(1)
        ).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )
        successCaptor.lastValue(verificationPage)
    }

    @Test
    fun `when waiting verificationPage UI shows progress circular`() {
        launchConsentFragment { binding, _ ->
            assertThat(binding.loadings.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.texts.visibility).isEqualTo(View.GONE)
            assertThat(binding.buttons.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `when not missing biometricConsent navigate to docSelectionFragment`() {
        whenever(verificationPage.requirements).thenReturn(
            VerificationPageRequirements(
                missing = emptyList()
            )
        )
        launchConsentFragment { _, navController ->
            setUpSuccessVerificationPage()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.docSelectionFragment)
        }
    }

    @Test
    fun `when verificationPage is ready UI is bound correctly`() {
        launchConsentFragment { binding, _ ->
            setUpSuccessVerificationPage()

            assertThat(binding.loadings.visibility).isEqualTo(View.GONE)
            assertThat(binding.texts.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.buttons.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.titleText.text).isEqualTo(CONSENT_TITLE)
            assertThat(binding.privacyPolicy.text.toString()).isEqualTo(CONSENT_PRIVACY_POLICY)
            assertThat(binding.timeEstimate.text).isEqualTo(CONSENT_TIME_ESTIMATE)
            assertThat(binding.body.text.toString()).isEqualTo(CONSENT_BODY)
            assertThat(binding.agree.findViewById<MaterialButton>(R.id.button).text).isEqualTo(
                CONSENT_ACCEPT_TEXT
            )
            assertThat(binding.decline.findViewById<MaterialButton>(R.id.button).text).isEqualTo(
                CONSENT_DECLINE_TEXT
            )
        }
    }

    @Test
    fun `when verificationApiErrorLiveData is ready transitions to errorFragment`() {
        launchConsentFragment { _, navController ->
            setUpErrorVerificationPage()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `when accepted and postVerificationData success transitions to docSelectionFragment`() {
        runBlocking {
            whenever(
                mockIdentityViewModel.postVerificationPageData(any(), any())
            ).thenReturn(correctVerificationData)

            launchConsentFragment { binding, navController ->
                setUpSuccessVerificationPage()

                binding.agree.findViewById<MaterialButton>(R.id.button).callOnClick()

                assertThat(binding.agree.findViewById<MaterialButton>(R.id.button).isEnabled).isFalse()
                assertThat(binding.agree.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )
                assertThat(binding.decline.isEnabled).isFalse()
                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.docSelectionFragment)
            }
        }
    }

    @Test
    fun `when accepted and postVerificationData fails transitions to errorFragment`() {
        runBlocking {
            whenever(
                mockIdentityViewModel.postVerificationPageData(any(), any())
            ).thenThrow(APIException())

            launchConsentFragment { binding, navController ->
                setUpSuccessVerificationPage()
                binding.agree.findViewById<MaterialButton>(R.id.button).callOnClick()

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `when declined and postVerificationData success transitions to errorFragment with returned value`() {
        runBlocking {
            whenever(
                mockIdentityViewModel.postVerificationPageData(any(), any())
            ).thenReturn(incorrectVerificationData)

            launchConsentFragment { binding, navController ->
                setUpSuccessVerificationPage()
                binding.decline.findViewById<MaterialButton>(R.id.button).callOnClick()

                assertThat(binding.decline.findViewById<MaterialButton>(R.id.button).isEnabled).isFalse()
                assertThat(binding.decline.findViewById<CircularProgressIndicator>(R.id.indicator).visibility).isEqualTo(
                    View.VISIBLE
                )
                assertThat(binding.agree.isEnabled).isFalse()
                requireNotNull(navController.backStack.last().arguments).let { arguments ->
                    assertThat(arguments[ErrorFragment.ARG_ERROR_TITLE])
                        .isEqualTo(ERROR_TITLE)
                    assertThat(arguments[ErrorFragment.ARG_ERROR_CONTENT])
                        .isEqualTo(ERROR_BODY)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION])
                        .isEqualTo(R.id.action_errorFragment_to_consentFragment)
                    assertThat(arguments[ErrorFragment.ARG_GO_BACK_BUTTON_TEXT])
                        .isEqualTo(ERROR_BUTTON_TEXT)
                }

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    @Test
    fun `when declined and postVerificationData fails transitions to errorFragment`() {
        runBlocking {
            whenever(
                mockIdentityViewModel.postVerificationPageData(any(), any())
            ).thenThrow(APIException())

            launchConsentFragment { binding, navController ->
                setUpSuccessVerificationPage()
                binding.decline.findViewById<MaterialButton>(R.id.button).callOnClick()

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.errorFragment)
            }
        }
    }

    private fun launchConsentFragment(
        testBlock: (binding: ConsentFragmentBinding, navController: TestNavHostController) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ConsentFragment(viewModelFactoryFor(mockIdentityViewModel))
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.consentFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        testBlock(ConsentFragmentBinding.bind(it.requireView()), navController)
    }

    private companion object {
        const val CONSENT_TITLE = "title"
        const val CONSENT_PRIVACY_POLICY = "privacy policy"
        const val CONSENT_TIME_ESTIMATE = "time estimate"
        const val CONSENT_BODY = "this is the consent body"
        const val CONSENT_ACCEPT_TEXT = "yes"
        const val CONSENT_DECLINE_TEXT = "no"

        const val ERROR_BODY = "error body"
        const val ERROR_BUTTON_TEXT = "go back to consent"

        const val ERROR_TITLE = "error title"

        const val VERIFICATION_SESSION_ID = "id_5678"
        const val EPHEMERAL_KEY = "eak_5678"
        val BRAND_LOGO = mock<Uri>()
    }
}
