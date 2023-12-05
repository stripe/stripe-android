package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.res.stringResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class WalletsDividerScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            WalletsDivider(
                text = stringResource(R.string.stripe_paymentsheet_or_pay_with_card),
            )
        }
    }
}
