package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.DefaultStripeTheme
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
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertThat(counter).isEqualTo(1)
    }

    private fun setContent(
        primaryButtonEnabled: Boolean = true,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            DefaultStripeTheme {
                InputAddressScreen(
                    primaryButtonEnabled = primaryButtonEnabled,
                    primaryButtonText = "Save Address",
                    title = "Address",
                    onPrimaryButtonClick = primaryButtonCallback,
                    onCloseClick = onCloseCallback,
                    formContent = {},
                    checkboxContent = {}
                )
            }
        }
    }
}
