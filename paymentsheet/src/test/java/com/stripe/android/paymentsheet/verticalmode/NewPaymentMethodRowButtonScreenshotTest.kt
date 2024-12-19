package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class NewPaymentMethodRowButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testSelectedState() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Card",
                subtitle = null,
                promoText = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testUnselectedState() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Card",
                subtitle = null,
                promoText = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = false,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Card",
                subtitle = null,
                promoText = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testLongTitle() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "The Greatest US Bank Account",
                subtitle = null,
                promoText = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testSubtitle() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Klarna",
                subtitle = "Buy now or pay later with Klarna.",
                promoText = null,
                iconRequiresTinting = false,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testLongSubtitle() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = "Klarna",
                subtitle = "A very long subtitle, that you should read all of and you should Buy now or pay later" +
                    " with Klarna.",
                iconRequiresTinting = false,
                promoText = null,
                onClick = {},
                modifier = Modifier,
            )
        }
    }
}
