package com.stripe.android.paymentsheet.addresselement

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.elements.AddressTextFieldController
import com.stripe.android.uicore.elements.AddressTextFieldUI
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.uicore.R as UiCoreR

@RunWith(AndroidJUnit4::class)
class AddressTextFieldUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clicking_address_should_trigger_on_cick() {
        var count = 0
        setContent {
            count++
        }

        composeTestRule.onNodeWithText("Address").performClick()

        Truth.assertThat(count).isEqualTo(1)
    }

    private fun setContent(
        onClick: () -> Unit
    ) {
        composeTestRule.setContent {
            DefaultStripeTheme {
                AddressTextFieldUI(
                    controller = AddressTextFieldController(
                        SimpleTextFieldConfig(label = UiCoreR.string.stripe_address_label_address)
                    ),
                    onClick = onClick
                )
            }
        }
    }
}
