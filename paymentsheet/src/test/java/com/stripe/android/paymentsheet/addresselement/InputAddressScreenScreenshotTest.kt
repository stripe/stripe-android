package com.stripe.android.paymentsheet.addresselement

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class InputAddressScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            screen(primaryButtonEnabled = true)
        }
    }

    @Test
    fun customized() {
        paparazziRule.snapshot {
            screen(
                primaryButtonEnabled = true,
                title = "Deliver to",
                buttonTitle = "Use this address",
            )
        }
    }

    @Test
    fun loading() {
        paparazziRule.snapshot {
            screen(
                primaryButtonEnabled = false,
                formText = "Saving address...",
            )
        }
    }

    @Test
    fun error() {
        paparazziRule.snapshot {
            screen(
                primaryButtonEnabled = true,
                formText = "We couldn't save your address. Please try again.",
            )
        }
    }

    @Composable
    private fun screen(
        primaryButtonEnabled: Boolean,
        title: String = "Shipping address",
        buttonTitle: String = "Save address",
        formText: String = "Address form",
    ) {
        InputAddressScreen(
            primaryButtonEnabled = primaryButtonEnabled,
            primaryButtonText = buttonTitle,
            title = title,
            onPrimaryButtonClick = {},
            onDisabledButtonClick = {},
            onCloseClick = {},
            topContent = {},
            formContent = { Text(formText) },
            bottomContent = {},
        )
    }
}
