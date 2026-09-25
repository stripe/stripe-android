@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertThat(counter).isEqualTo(1)
    }

    private fun setContent(
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        primaryButtonEnabled: Boolean = true,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {},
        formContent: @Composable ColumnScope.() -> Unit = {},
    ) {
        composeTestRule.setContent {
            InputAddressScreen(
                appearance = appearance,
                primaryButtonEnabled = primaryButtonEnabled,
                primaryButtonLoading = false,
                primaryButtonText = "Save Address",
                title = "Address",
                onPrimaryButtonClick = primaryButtonCallback,
                onDisabledButtonClick = {},
                onCloseClick = onCloseCallback,
                topContent = {},
                formContent = formContent,
                bottomContent = {},
            )
        }
    }
}
