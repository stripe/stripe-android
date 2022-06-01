package com.stripe.android.link.ui.cardedit

import android.content.Intent
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.StripeIntentFixtures
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CardEditScreenTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args(
                    StripeIntentFixtures.PI_SUCCEEDED,
                    true,
                    "Merchant, Inc"
                )
            )
        }
    }

    @Test
    fun when_default_show_default_label() {
        setContent(isDefault = true)
        onDefaultLabel().assertExists()
        onSetAsDefaultLabel().assertDoesNotExist()
    }

    @Test
    fun when_not_default_show_checkbox() {
        setContent(isDefault = false)
        onDefaultLabel().assertDoesNotExist()
        onSetAsDefaultLabel().assertExists()
    }

    @Test
    fun when_set_as_default_checkbox_clicked_callback_is_triggered() {
        var setAsDefaultChecked = false
        setContent(
            isDefault = false,
            setAsDefaultChecked = setAsDefaultChecked,
            onSetAsDefaultClick = {
                setAsDefaultChecked = it
            }
        )
        assertThat(setAsDefaultChecked).isFalse()
        onSetAsDefaultLabel().performClick()
        assertThat(setAsDefaultChecked).isTrue()
    }

    @Test
    fun update_card_button_triggers_action() {
        var count = 0
        setContent(onPrimaryButtonClick = {
            count++
        })
        onUpdateCardButton().performClick()
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun cancel_button_triggers_action() {
        var count = 0
        setContent(onCancelClick = {
            count++
        })
        onCancelButton().performClick()
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun when_error_message_is_not_null_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        isProcessing: Boolean = false,
        isDefault: Boolean = false,
        setAsDefaultChecked: Boolean = false,
        primaryButtonEnabled: Boolean = true,
        errorMessage: ErrorMessage? = null,
        onSetAsDefaultClick: (Boolean) -> Unit = {},
        onPrimaryButtonClick: () -> Unit = {},
        onCancelClick: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            CardEditBody(
                isProcessing = isProcessing,
                isDefault = isDefault,
                setAsDefaultChecked = setAsDefaultChecked,
                primaryButtonEnabled = primaryButtonEnabled,
                errorMessage = errorMessage,
                onSetAsDefaultClick = onSetAsDefaultClick,
                onPrimaryButtonClick = onPrimaryButtonClick,
                onCancelClick = onCancelClick,
                formContent = {}
            )
        }
    }

    private fun onDefaultLabel() = composeTestRule.onNodeWithText("This is your default")
    private fun onSetAsDefaultLabel() = composeTestRule.onNodeWithText("Set as default payment")
    private fun onUpdateCardButton() =
        composeTestRule.onNode(
            hasText("Update card").and(hasParent(hasClickAction())),
            useUnmergedTree = true
        )

    private fun onCancelButton() = composeTestRule.onNodeWithText("Cancel")
}
