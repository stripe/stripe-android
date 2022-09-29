package com.stripe.android.link.ui.cardedit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CardEditScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun when_default_show_show_disabled_checkbox() {
        setContent(isDefault = true)
        onDefaultCheckboxRow().assertIsNotEnabled()
    }

    @Test
    fun when_not_default_show_enabled_checkbox() {
        setContent(isDefault = false)
        onDefaultCheckboxRow().assertIsEnabled()
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

    private fun onDefaultCheckboxRow() = composeTestRule.onNodeWithTag(
        testTag = DEFAULT_PAYMENT_METHOD_CHECKBOX_TAG
    )

    private fun onSetAsDefaultLabel() = composeTestRule.onNodeWithText("Set as default payment method")

    private fun onUpdateCardButton() = composeTestRule.onNode(
        matcher = hasText("Update card").and(hasParent(hasClickAction())),
        useUnmergedTree = true
    )

    private fun onCancelButton() = composeTestRule.onNodeWithText("Cancel")
}
