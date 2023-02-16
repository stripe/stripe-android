package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.IndividualDestination
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

    private val verificationPageForDocType = mock<VerificationPage>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(
                    Requirement.BIOMETRICCONSENT,
                    Requirement.IDDOCUMENTTYPE,
                    Requirement.IDDOCUMENTFRONT,
                    Requirement.IDDOCUMENTBACK,
                    Requirement.IDNUMBER
                )
            )
        )
    }

    private val verificationPageForIdNumberType = mock<VerificationPage>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(Requirement.IDNUMBER)
            )
        )
    }
    private val verificationPageForAddressType = mock<VerificationPage>().also {
        whenever(it.requirements).thenReturn(
            VerificationPageRequirements(
                missing = listOf(Requirement.ADDRESS)
            )
        )
    }

    private val verificationPageData = MutableLiveData<Resource<VerificationPage>>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageData
    }
    private val mockNavController = mock<NavController>()

    @Test
    fun testDocTypeNavigatesToConsent() {
        setComposeTestRuleWith(verificationPageForDocType) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(ConsentDestination.CONSENT)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testIdNumberTypeNavigatesToIndividual() {
        setComposeTestRuleWith(verificationPageForIdNumberType) {
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(IndividualDestination.INDIVIDUAL)
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
                    it.startsWith(IndividualDestination.INDIVIDUAL)
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
            )
        }

        with(composeTestRule, testBlock)
    }
}
