package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.material.MaterialTheme
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.stripeFormInsets
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
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = null,
                ),
                horizontalPadding = MaterialTheme.stripeFormInsets.getOuterFormInsets(),
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
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = null,
                ),
                horizontalPadding = MaterialTheme.stripeFormInsets.getOuterFormInsets(),
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
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    promoBadge = "$5",
                ),
                horizontalPadding = MaterialTheme.stripeFormInsets.getOuterFormInsets(),
            )
        }
    }
}
