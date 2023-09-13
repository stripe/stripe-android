package com.stripe.android.uicore.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.PaparazziRule
import com.stripe.android.uicore.utils.SystemAppearance
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
        SystemAppearance.values()
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
