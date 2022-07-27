package com.stripe.android.link.ui.paymentmethod

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completedIconTestTag
import com.stripe.android.link.ui.progressIndicatorTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PaymentMethodScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val primaryButtonLabel = "Pay $10.99"
    private val secondaryButtonLabel = "Cancel"

    @Test
    fun primary_button_shows_progress_indicator_when_processing() {
        setContent(primaryButtonState = PrimaryButtonState.Processing)
        onProgressIndicator().assertExists()
    }

    @Test
    fun primary_button_shows_checkmark_when_completed() {
        setContent(primaryButtonState = PrimaryButtonState.Completed)
        onCompletedIcon().assertExists()
    }

    @Test
    fun buttons_are_disabled_when_processing() {
        var count = 0
        setContent(
            primaryButtonState = PrimaryButtonState.Processing,
            onPayButtonClick = {
                count++
            },
            onSecondaryButtonClick = {
                count++
            }
        )

        onPrimaryButton().assertDoesNotExist()
        onSecondaryButton().performClick()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun primary_button_does_not_trigger_event_when_disabled() {
        var count = 0
        setContent(
            primaryButtonState = PrimaryButtonState.Processing,
            onPayButtonClick = {
                count++
            }
        )

        onProgressIndicator().performClick()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun secondary_button_click_triggers_action() {
        var count = 0
        setContent(
            onSecondaryButtonClick = {
                count++
            }
        )

        onSecondaryButton().performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun when_error_message_is_not_null_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        primaryButtonState: PrimaryButtonState = PrimaryButtonState.Enabled,
        errorMessage: ErrorMessage? = null,
        onPayButtonClick: () -> Unit = {},
        onSecondaryButtonClick: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            PaymentMethodBody(
                primaryButtonLabel = primaryButtonLabel,
                primaryButtonState = primaryButtonState,
                secondaryButtonLabel = secondaryButtonLabel,
                errorMessage = errorMessage,
                onPrimaryButtonClick = onPayButtonClick,
                onSecondaryButtonClick = onSecondaryButtonClick,
                formContent = {}
            )
        }
    }

    private fun onPrimaryButton() = composeTestRule.onNodeWithText(primaryButtonLabel)
    private fun onSecondaryButton() = composeTestRule.onNodeWithText(secondaryButtonLabel)
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(progressIndicatorTestTag)
    private fun onCompletedIcon() = composeTestRule.onNodeWithTag(completedIconTestTag, true)
}
