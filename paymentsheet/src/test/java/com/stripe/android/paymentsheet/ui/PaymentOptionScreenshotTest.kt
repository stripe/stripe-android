
package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class PaymentOptionScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun testEnabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = false,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = false,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = false,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = false,
                isEnabled = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testSelected() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = true,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = false,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testSelectedAndDisabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = true,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = false,
                isEnabled = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testModifying() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = false,
                shouldShowModifyBadge = true,
                shouldShowDefaultBadge = false,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDefaultNotEditing() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = false,
                shouldShowModifyBadge = false,
                shouldShowDefaultBadge = true,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••8431",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testDefaultEditing() {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                viewWidth = 160.dp,
                isSelected = false,
                shouldShowModifyBadge = true,
                shouldShowDefaultBadge = true,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••8431",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }
}
