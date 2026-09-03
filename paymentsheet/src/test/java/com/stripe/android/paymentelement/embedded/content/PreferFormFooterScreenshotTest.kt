package com.stripe.android.paymentelement.embedded.content

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import org.junit.Rule
import kotlin.test.Test

internal class PreferFormFooterScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun displaysMorePaymentMethodsFooter() {
        paparazziRule.snapshot {
            PaymentElementTheme(appearance = PaymentSheet.Appearance()) {
                PreferFormFooter(
                    alternatives = alternatives,
                    enabled = true,
                    onClick = {},
                )
            }
        }
    }

    private val alternatives = listOf(
        paymentMethod("card", R.drawable.stripe_ic_paymentsheet_pm_card),
        paymentMethod("klarna", R.drawable.stripe_ic_paymentsheet_pm_klarna),
        paymentMethod("affirm", R.drawable.stripe_ic_paymentsheet_pm_affirm),
        paymentMethod("afterpay_clearpay", R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay),
    )

    private fun paymentMethod(code: String, iconResource: Int): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = code,
            displayName = code.resolvableString,
            iconResource = iconResource,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = false,
        )
    }
}
