package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.TestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class SelfieWarmupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyContentVisibleAndContinueClickWithoutTrainingConsent() {
        var trainingConsent: Boolean? = true
        composeTestRule.setContent {
            SelfieWarmupView(trainingConsentText = null) {
                trainingConsent = it
            }
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_WARMUP_CONTENT_TAG).assertExists()

            onNodeWithTag(SELFIE_CONTINUE_BUTTON_TAG).onChildAt(0).performClick()
            assertThat(trainingConsent).isNull()
        }
    }

    @Test
    fun verifyAllowClickWithTrainingConsent() {
        var trainingConsent: Boolean? = null
        composeTestRule.setContent {
            SelfieWarmupView(trainingConsentText = "<a href=\"https://stripe.com\">consent</a>") {
                trainingConsent = it
            }
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_TRAINING_CONSENT_TAG).assertExists()
            onNodeWithTag(SELFIE_ALLOW_BUTTON_TAG).onChildAt(0).performClick()
            assertThat(trainingConsent).isTrue()
        }
    }

    @Test
    fun verifyDeclineClickWithTrainingConsent() {
        var trainingConsent: Boolean? = null
        composeTestRule.setContent {
            SelfieWarmupView(trainingConsentText = "<a href=\"https://stripe.com\">consent</a>") {
                trainingConsent = it
            }
        }

        with(composeTestRule) {
            onNodeWithTag(SELFIE_TRAINING_CONSENT_TAG).assertExists()
            onNodeWithTag(SELFIE_DECLINE_BUTTON_TAG).onChildAt(0).performClick()
            assertThat(trainingConsent).isFalse()
        }
    }
}
