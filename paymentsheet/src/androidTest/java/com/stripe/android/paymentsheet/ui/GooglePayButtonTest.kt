package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class GooglePayButtonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun handlesPressWhenEnabled() {
        var didCallOnPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GooglePayButton(
                    state = PrimaryButton.State.Ready,
                    isEnabled = true,
                    allowCreditCards = true,
                    billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                    onPressed = { didCallOnPressed = true },
                )
            }
        }

        composeTestRule
            .onNodeWithTag(GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isTrue()
    }

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun ignoresPressWhenDisabled() {
        var didCallOnPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GooglePayButton(
                    state = PrimaryButton.State.Ready,
                    isEnabled = false,
                    allowCreditCards = true,
                    billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                    onPressed = { didCallOnPressed = true },
                )
            }
        }

        composeTestRule
            .onNodeWithTag(GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isFalse()
    }

    @Test
    fun showPayButtonInReadyState() {
        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                isEnabled = true,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = {},
            )
        }

        composeTestRule
            .onNodeWithTag(GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(GOOGLE_PAY_BUTTON_PRIMARY_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun usesCorrectAlphaIfDisabled() {
        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                isEnabled = false,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = {},
            )
        }

        composeTestRule
            .onNodeWithTag(GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG)
            .assertAlpha(0.38f)
    }
}

private fun SemanticsNodeInteraction.assertAlpha(
    alpha: Float,
): SemanticsNodeInteraction {
    return assert(hasAlpha(alpha))
}

private fun hasAlpha(alpha: Float): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} has alpha '$alpha'"
) { node ->
    node.layoutInfo.getModifierInfo().filter { modifierInfo ->
        modifierInfo.modifier == Modifier.alpha(alpha)
    }.size == 1
}
