package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class PhoneNumberElementUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(PaddingValues(vertical = 16.dp))
            .fillMaxWidth()
    )

    @Test
    fun testValidNumber() {
        paparazziRule.snapshot {
            TestPhoneNumberElementUI(
                initialValue = "+11234567890",
                initialCountryCode = "CA",
            )
        }
    }

    @Test
    fun testIncompleteNumber() {
        paparazziRule.snapshot {
            TestPhoneNumberElementUI(
                initialValue = "+11234567",
                initialCountryCode = "US",
            )
        }
    }

    @Composable
    private fun TestPhoneNumberElementUI(
        initialValue: String,
        initialCountryCode: String,
    ) {
        PhoneNumberElementUI(
            enabled = true,
            controller = PhoneNumberController.createPhoneNumberController(
                initiallySelectedCountryCode = initialCountryCode,
                initialValue = initialValue,
            )
        )
    }
}
