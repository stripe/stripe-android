package com.stripe.android.identity.navigation

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.ConsentFragmentViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class ConsentFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val verificationPageWithTimeAndPolicy = mock<VerificationPage>().also {
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

    private val mockScreenTracker = mock<ScreenTracker>()

    private val verificationPageData = MutableLiveData<Resource<VerificationPage>>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationArgs }.thenReturn(ARGS)

        on { identityAnalyticsRequestFactory }.thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = ARGS
            )
        )
        on { screenTracker }.thenReturn(mockScreenTracker)
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
        on { verificationPage } doReturn verificationPageData
    }

    private val mockConsentFragmentViewModel = mock<ConsentFragmentViewModel>()

    @Test
    fun `when viewCreated track screen finish`() {
        verificationPageData.postValue(Resource.success(verificationPageWithTimeAndPolicy))

        launchConsentFragment { _, _ ->
            runBlocking {
                verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_CONSENT))
            }

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_CONSENT
                }
            )
        }
    }

    private fun launchConsentFragment(
        testBlock: (navController: TestNavHostController, fragment: ConsentFragment) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ConsentFragment(
            viewModelFactoryFor(mockIdentityViewModel),
            viewModelFactoryFor(mockConsentFragmentViewModel),
            mock()
        )
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
        testBlock(navController, it)
    }

    private companion object {
        const val CONSENT_TITLE = "title"
        const val CONSENT_PRIVACY_POLICY = "privacy policy"
        const val CONSENT_TIME_ESTIMATE = "time estimate"
        const val CONSENT_BODY = "this is the consent body"
        const val CONSENT_ACCEPT_TEXT = "yes"
        const val CONSENT_DECLINE_TEXT = "no"

        const val VERIFICATION_SESSION_ID = "id_5678"
        const val EPHEMERAL_KEY = "eak_5678"

        val BRAND_LOGO = mock<Uri>()

        val ARGS = IdentityVerificationSheetContract.Args(
            verificationSessionId = VERIFICATION_SESSION_ID,
            ephemeralKeySecret = EPHEMERAL_KEY,
            brandLogo = BRAND_LOGO,
            injectorKey = DUMMY_INJECTOR_KEY,
            presentTime = 0
        )
    }
}
