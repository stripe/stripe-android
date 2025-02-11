package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import org.junit.Rule
import org.junit.Test

internal class VerticalModeFormHeaderUITest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testLpm() {
        paparazziRule.snapshot {
            VerticalModeFormHeaderUI(
                isEnabled = true,
                formHeaderInformation = FormHeaderInformation(
                    displayName = "Cash App Pay".resolvableString,
                    shouldShowIcon = true,
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = null,
                )
            )
        }
    }

    @Test
    fun testCard() {
        paparazziRule.snapshot {
            VerticalModeFormHeaderUI(
                isEnabled = true,
                formHeaderInformation = FormHeaderInformation(
                    displayName = "Add new card".resolvableString,
                    shouldShowIcon = false,
                    iconResource = 0,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = null,
                )
            )
        }
    }

    @Test
    fun testBank() {
        paparazziRule.snapshot {
            VerticalModeFormHeaderUI(
                isEnabled = true,
                formHeaderInformation = FormHeaderInformation(
                    displayName = "Bank".resolvableString,
                    shouldShowIcon = true,
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = "$5",
                )
            )
        }
    }
}
