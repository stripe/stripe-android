package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class PaymentMethodUIScreenshotTest {
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
            PaymentMethodUI(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = true,
                isEnabled = true,
                iconRequiresTinting = true,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testUnselectedState() {
        paparazziRule.snapshot {
            PaymentMethodUI(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = true,
                iconRequiresTinting = true,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            PaymentMethodUI(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = false,
                iconRequiresTinting = true,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testLongTitle() {
        paparazziRule.snapshot {
            PaymentMethodUI(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
                iconUrl = null,
                imageLoader = mock(),
                title = "The Greatest US Bank Account",
                isSelected = false,
                isEnabled = false,
                iconRequiresTinting = true,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testSelectedStateForVerticalMode() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                subTitle = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testUnselectedStateForVerticalMode() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                subTitle = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testDisabledForVerticalMode() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = false,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                subTitle = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }

    @Test
    fun testLongTitleForVerticalMode() {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
                iconUrl = null,
                imageLoader = mock(),
                title = "The Greatest US Bank Account",
                subTitle = null,
                iconRequiresTinting = true,
                onClick = {},
                modifier = Modifier,
            )
        }
    }
}
