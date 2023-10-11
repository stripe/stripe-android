package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentTextPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class ConfirmationScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.success).thenReturn(
            VerificationPageStaticContentTextPage(
                body = CONFIRMATION_BODY,
                buttonText = CONFIRMATION_BUTTON_TEXT,
                title = CONFIRMATION_TITLE
            )
        )
    }

    private val mockIdentityViewModel = mock<IdentityViewModel>() {
        on { verificationPage } doReturn MutableLiveData(Resource.success(verificationPage))
    }
    private val mockNavController = mock<NavController>()
    private val mockVerificationFlowFinishable = mock<VerificationFlowFinishable>()

    @Test
    fun verifyUIIsBoundAndButtonInteracts() {
        testConfirmationScreen {
            onNodeWithTag(CONFIRMATION_TITLE_TAG).assertTextEquals(CONFIRMATION_TITLE)
            onNodeWithTag(CONFIRMATION_BODY_TAG).assertTextEquals(CONFIRMATION_BODY)
            onNodeWithTag(CONFIRMATION_BUTTON_TAG).assertTextEquals(
                CONFIRMATION_BUTTON_TEXT.uppercase()
            )
            onNodeWithTag(CONFIRMATION_BUTTON_TAG).performClick()
            verify(mockIdentityViewModel).sendSucceededAnalyticsRequestForNative()
            verify(mockVerificationFlowFinishable).finishWithResult(
                eq(IdentityVerificationSheet.VerificationFlowResult.Completed)
            )
        }
    }

    private fun testConfirmationScreen(
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            ConfirmationScreen(
                mockNavController,
                mockIdentityViewModel,
                mockVerificationFlowFinishable
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val CONFIRMATION_TITLE = "title"
        const val CONFIRMATION_BODY = "this is the confirmation body"
        const val CONFIRMATION_BUTTON_TEXT = "confirmation"
    }
}
