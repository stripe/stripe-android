package com.stripe.android.paymentsheet.addresselement

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.stripe.android.R
import com.stripe.android.ui.core.DefaultPaymentsTheme
import com.stripe.android.ui.core.elements.AddressTextFieldController
import com.stripe.android.ui.core.elements.AddressTextFieldUI
import com.stripe.android.ui.core.elements.SimpleTextFieldConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            DefaultPaymentsTheme {
                AddressTextFieldUI(
                    controller = AddressTextFieldController(
                        SimpleTextFieldConfig(label = R.string.address_label_address)
                    ),
                    onClick = onClick
                )
            }
        }
    }
}
