package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.navigation.INDIVIDUAL
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentCountryNotListedPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class CountryNotListedScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.countryNotListedPage).thenReturn(
            VerificationPageStaticContentCountryNotListedPage(
                title = TITLE,
                body = BODY,
                cancelButtonText = CANCEL_BUTTON_TEXT,
                idFromOtherCountryTextButtonText = ID_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT,
                addressFromOtherCountryTextButtonText = ADDRESS_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT
            )
        )
    }

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn MutableLiveData(Resource.success(verificationPage))
    }

    private val mockNavController = mock<NavController>()

    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()

    @Test
    fun verifyUIForMissinIdNumberClickingOtherCountry() {
        testClickOtherCountryButton(isMissingID = true)
    }

    @Test
    fun verifyUIForMissingIdNumberClickingCancel() {
        testClickCancelButton(isMissingID = true)
    }

    @Test
    fun verifyUIForMissingCountryClickingOtherCountry() {
        testClickOtherCountryButton(isMissingID = false)
    }

    @Test
    fun verifyUIForMissingCountryClickingCancel() {
        testClickCancelButton(isMissingID = false)
    }

    private fun testClickOtherCountryButton(
        isMissingID: Boolean
    ) {
        setComposeTestRuleWith(isMissingID) {
            onNodeWithTag(COUNTRY_NOT_LISTED_OTHER_COUNTRY_TAG).performClick()
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(INDIVIDUAL)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun testClickCancelButton(
        isMissingID: Boolean
    ) {
        setComposeTestRuleWith(isMissingID) {
            onNodeWithTag(COUNTRY_NOT_LISTED_CANCEL_BUTTON_TAG).performClick()
            verify(mockVerificationFlowFinishable).finishWithResult(
                same(IdentityVerificationSheet.VerificationFlowResult.Canceled)
            )
        }
    }

    private fun setComposeTestRuleWith(
        isMissingID: Boolean,
        clickAction: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            CountryNotListedScreen(
                isMissingID = isMissingID,
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                verificationFlowFinishable = mockVerificationFlowFinishable
            )
        }

        with(composeTestRule) {
            onNodeWithTag(COUNTRY_NOT_LISTED_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(COUNTRY_NOT_LISTED_BODY_TAG).assertTextEquals(BODY)
            onNodeWithTag(COUNTRY_NOT_LISTED_OTHER_COUNTRY_TAG).assertTextEquals(
                if (isMissingID) {
                    ID_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT
                } else {
                    ADDRESS_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT
                }
            )
            onNodeWithTag(COUNTRY_NOT_LISTED_CANCEL_BUTTON_TAG).assertTextEquals(CANCEL_BUTTON_TEXT.uppercase())

            with(composeTestRule, clickAction)
        }
    }

    private companion object {
        private const val TITLE = "We can’t verify your identity"
        private const val BODY =
            "The countries not listed are not supported yet. Unfortunately, we cannot verify your identity."
        private const val CANCEL_BUTTON_TEXT = "Cancel verification"
        private const val ID_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT =
            "Have an ID from another country?"
        private const val ADDRESS_FROM_OTHER_COUNTRY_TEXT_BUTTON_TEXT =
            "Have an Address from another country?"
    }
}
