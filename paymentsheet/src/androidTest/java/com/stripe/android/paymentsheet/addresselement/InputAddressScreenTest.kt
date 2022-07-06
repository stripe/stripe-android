package com.stripe.android.paymentsheet.addresselement

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.stripe.android.ui.core.DefaultPaymentsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
class InputAddressScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun no_shipping_address_set_should_show_expand_button() {
        setContent()
        composeTestRule.onNodeWithText("Enter manually").assertExists()
    }

    @Test
    fun shipping_address_set_should_not_show_expand_button() {
        setContent(ShippingAddress(name = "skyler"))
        composeTestRule.onNodeWithText("Enter manually").assertDoesNotExist()
    }

    @Test
    fun clicking_primary_button_triggers_callback_when_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = true, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        Truth.assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_primary_button_does_not_trigger_callback_when_not_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = false, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        Truth.assertThat(counter).isEqualTo(0)
    }

    @Test
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        Truth.assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_enter_manually_triggers_callback() {
        var counter = 0
        setContent(onEnterManuallyCallback = { counter++ })
        composeTestRule.onNodeWithText("Enter Manually").performClick()
        Truth.assertThat(counter).isEqualTo(1)
    }

    private fun setContent(
        collectedAddress: ShippingAddress? = null,
        primaryButtonEnabled: Boolean = true,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {},
        onEnterManuallyCallback: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            DefaultPaymentsTheme {
                InputAddressScreen(
                    collectedAddress = collectedAddress,
                    primaryButtonEnabled = primaryButtonEnabled,
                    onPrimaryButtonClick = primaryButtonCallback,
                    onCloseClick = onCloseCallback,
                    onEnterManuallyClick = onEnterManuallyCallback,
                    formContent = {}
                )
            }
        }
    }
}
