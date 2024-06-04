package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.StripeTheme
import org.junit.Rule
import org.junit.Test

class CheckboxFieldUIViewScreenshotTest {
    private val label =
        @Composable
        @ReadOnlyComposable
        {
            "I understand that Stripe will be collecting Direct Debits on behalf of " +
                "Test Business Name and confirm that I am the account holder and the only " +
                "person required to authorise debits from this account."
        }

    private val error =
        @Composable
        @ReadOnlyComposable
        {
            "This is required"
        }

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(PaddingValues(vertical = 16.dp))
            .fillMaxWidth()
    )

    @Test
    fun testCheckboxEnabled() {
        paparazziRule.snapshot {
            StripeTheme {
                CheckboxFieldUIView(
                    enabled = true,
                    isChecked = false,
                    debugTag = "",
                    error = null,
                    label = label,
                    onValueChange = {}
                )
            }
        }
    }

    @Test
    fun testCheckboxDisabled() {
        paparazziRule.snapshot {
            CheckboxFieldUIView(
                enabled = true,
                isChecked = false,
                debugTag = "",
                error = null,
                label = label,
                onValueChange = {}
            )
        }
    }

    @Test
    fun testCheckboxChecked() {
        paparazziRule.snapshot {
            CheckboxFieldUIView(
                enabled = true,
                isChecked = true,
                debugTag = "",
                error = null,
                label = label,
                onValueChange = {}
            )
        }
    }

    @Test
    fun testCheckboxError() {
        paparazziRule.snapshot {
            CheckboxFieldUIView(
                enabled = true,
                isChecked = false,
                debugTag = "",
                error = error,
                label = label,
                onValueChange = {}
            )
        }
    }
}
