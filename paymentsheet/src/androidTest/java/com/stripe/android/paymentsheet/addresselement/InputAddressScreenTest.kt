package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.RequiresIme
import com.stripe.android.paymentsheet.assertNodeWithTagVisibleAboveKeyboard
import com.stripe.android.paymentsheet.waitForKeyboardToBeVisible
import com.stripe.android.uicore.DefaultStripeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputAddressScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun clicking_primary_button_triggers_callback_when_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = true, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithTag(ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG).assertIsEnabled()
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_primary_button_does_not_trigger_callback_when_not_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = false, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithTag(ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG).assertIsNotEnabled()
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

    @RequiresIme
    @Test
    fun primaryButtonIsVisibleAboveKeyboardWhenAddressBecomesComplete() {
        composeTestRule.setContent {
            var address by remember { mutableStateOf("") }

            DefaultStripeTheme {
                InputAddressScreen(
                    primaryButtonEnabled = address.isNotEmpty(),
                    primaryButtonText = "Save Address",
                    title = "Address",
                    onPrimaryButtonClick = {},
                    onDisabledButtonClick = {},
                    onCloseClick = {},
                    topContent = {},
                    formContent = {
                        Spacer(modifier = Modifier.height(800.dp))
                        TextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address line 1") },
                            modifier = Modifier.testTag(ADDRESS_FIELD_TEST_TAG),
                        )
                    },
                    bottomContent = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(ADDRESS_FIELD_TEST_TAG)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForKeyboardToBeVisible()
        composeTestRule.onNodeWithTag(ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG)
            .assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(ADDRESS_FIELD_TEST_TAG)
            .performTextInput("510 Townsend St")
        composeTestRule.onNodeWithTag(ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG)
            .assertIsEnabled()
        composeTestRule.assertNodeWithTagVisibleAboveKeyboard(
            testTag = ADDRESS_ELEMENT_PRIMARY_BUTTON_TEST_TAG,
        )
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
                    onDisabledButtonClick = {},
                    onCloseClick = onCloseCallback,
                    topContent = {},
                    formContent = {},
                    bottomContent = {}
                )
            }
        }
    }

    private companion object {
        const val ADDRESS_FIELD_TEST_TAG = "address_field"
    }
}
