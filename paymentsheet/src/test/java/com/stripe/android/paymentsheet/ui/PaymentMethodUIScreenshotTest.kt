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
    fun testSelectedStateForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = true,
                isEnabled = true,
                iconRequiresTinting = true,
                promoBadge = null,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testUnselectedStateForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = true,
                iconRequiresTinting = true,
                promoBadge = null,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testUnselectedStateWithBadgeForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = true,
                iconRequiresTinting = true,
                promoBadge = "$5",
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDisabledForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = false,
                iconRequiresTinting = true,
                promoBadge = null,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDisabledWithBadgeForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "Card",
                isSelected = false,
                isEnabled = false,
                iconRequiresTinting = true,
                promoBadge = "$5",
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testLongTitleForTabMode() {
        paparazziRule.snapshot {
            NewPaymentMethodTab(
                minViewWidth = 100.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
                iconUrl = null,
                imageLoader = mock(),
                title = "The Greatest US Bank Account",
                isSelected = false,
                isEnabled = false,
                iconRequiresTinting = true,
                promoBadge = null,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }
}
