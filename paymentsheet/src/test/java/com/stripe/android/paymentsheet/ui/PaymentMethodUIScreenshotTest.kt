package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentMethodUI
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class PaymentMethodUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
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
                tintOnSelected = true,
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
                tintOnSelected = true,
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
                tintOnSelected = true,
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
                iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
                iconUrl = null,
                imageLoader = mock(),
                title = "The Greatest US Bank Account",
                isSelected = false,
                isEnabled = false,
                tintOnSelected = true,
                modifier = Modifier,
                onItemSelectedListener = {},
            )
        }
    }
}
