@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputAddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clicking_primary_button_triggers_callback_when_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = true, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_primary_button_does_not_trigger_callback_when_not_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = false, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(0)
    }

    @Test
    fun loading_primary_button_displays_indicator_and_is_disabled() {
        setContent(primaryButtonEnabled = false, primaryButtonLoading = true)

        composeTestRule.onNodeWithText("Save Address").assertIsNotEnabled()
        composeTestRule.onNode(
            matcher = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun save_error_is_displayed() {
        setContent(saveError = "Something went wrong.".resolvableString)

        composeTestRule.onNodeWithText("Something went wrong.").assertIsDisplayed()
    }

    @Test
    fun save_error_is_not_displayed_when_null() {
        setContent(saveError = null)

        composeTestRule.onNodeWithText("Something went wrong.").assertDoesNotExist()
    }

    private fun setContent(
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        primaryButtonEnabled: Boolean = true,
        primaryButtonLoading: Boolean = false,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {},
        formContent: @Composable ColumnScope.() -> Unit = {},
        saveError: ResolvableString? = null,
    ) {
        composeTestRule.setContent {
            InputAddressScreen(
                appearance = appearance,
                primaryButtonEnabled = primaryButtonEnabled,
                primaryButtonLoading = primaryButtonLoading,
                primaryButtonText = "Save Address",
                title = "Address",
                onPrimaryButtonClick = primaryButtonCallback,
                onDisabledButtonClick = {},
                onCloseClick = onCloseCallback,
                topContent = {},
                formContent = formContent,
                bottomContent = {},
                saveError = saveError,
            )
        }
    }
}
