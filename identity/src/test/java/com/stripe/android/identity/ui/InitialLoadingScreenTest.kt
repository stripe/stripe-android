package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.DebugDestination
import com.stripe.android.identity.navigation.INDIVIDUAL
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class InitialLoadingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockFallbackUrlLauncher = mock<FallbackUrlLauncher>()

    private val verificationPageUnsupported = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(true)
        whenever(it.livemode).thenReturn(true)
        whenever(it.unsupportedClient).thenReturn(true)
        whenever(it.fallbackUrl).thenReturn(FALLBACK_URL)
    }

    private val verificationPageForIdNumberType = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(true)
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(Requirement.IDNUMBER)
            )
        )
    }
    private val verificationPageForAddressType = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(true)
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(Requirement.ADDRESS)
            )
        )
    }

    private val verificationPageWithEmptyRequirements = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(true)
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf()
            )
        )
    }

    private val verificationPageInTestMode = mock<VerificationPage>().also {
        whenever(it.livemode).thenReturn(false)
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    Requirement.BIOMETRICCONSENT,
                    Requirement.IDDOCUMENTFRONT,
                    Requirement.IDDOCUMENTBACK,
                    Requirement.IDNUMBER
                )
            )
        )
    }

    private val verificationPageData = MutableLiveData<Resource<VerificationPage>>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageData
    }
    private val mockNavController = mock<NavController>()

    @Test
    fun testUnsupportedClientOpensFallback() {
        setComposeTestRuleWith(verificationPageUnsupported) {
            verify(mockFallbackUrlLauncher).launchFallbackUrl(eq(FALLBACK_URL))
        }
    }

    @Test
    fun testIdNumberTypeNavigatesToIndividual() {
        setComposeTestRuleWith(verificationPageForIdNumberType) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(INDIVIDUAL)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testAddressTypeNavigatesToIndividual() {
        setComposeTestRuleWith(verificationPageForAddressType) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(INDIVIDUAL)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testEmptyRequirementsNavigatesToSuccess() {
        setComposeTestRuleWith(verificationPageWithEmptyRequirements) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(ConfirmationDestination.CONFIRMATION)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testTestModeNavigatesToDebugScreen() {
        setComposeTestRuleWith(verificationPageInTestMode) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(DebugDestination.DEBUG)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun setComposeTestRuleWith(
        verificationPage: VerificationPage,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        verificationPageData.postValue(Resource.success(verificationPage))
        composeTestRule.setContent {
            InitialLoadingScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                fallbackUrlLauncher = mockFallbackUrlLauncher
            )
        }

        with(composeTestRule, testBlock)
    }

    private companion object {
        const val FALLBACK_URL = "https://fallback/url"
    }
}
