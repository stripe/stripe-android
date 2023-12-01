package com.stripe.android.identity.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentSelectPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class DocWarmupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val onContinueClickMock: () -> Unit = mock()

    @Test
    fun verifyContentVisibleAndButtonClick() {
        composeTestRule.setContent {
            DocWarmupView(
                VerificationPageStaticContentDocumentSelectPage(
                    buttonText = "unused text",
                    idDocumentTypeAllowlist = mapOf(
                        "passport" to "Passport",
                        "driving_license" to "Driver's license",
                        "id_card" to "Identity card"
                    ),
                    title = "unused title",
                    body = "unused body"
                ),
                onContinueClickMock
            )
        }
        with(composeTestRule) {
            onNodeWithTag(DOC_FRONT_ACCEPTED_IDS_TAG).assertExists()
            onNodeWithTag(DOC_FRONT_ACCEPTED_IDS_TAG).assertTextEquals(
                "${context.getString(R.string.stripe_accepted_forms_of_id_include)} " +
                    "${context.getString(R.string.stripe_passport)}, " +
                    "${context.getString(R.string.stripe_driver_license)}, " +
                    "${context.getString(R.string.stripe_government_id)}."
            )
            onNodeWithTag(DOC_FRONT_CONTINUE_BUTTON_TAG).onChildAt(0).performClick()
            verify(onContinueClickMock).invoke()
        }
    }
}
