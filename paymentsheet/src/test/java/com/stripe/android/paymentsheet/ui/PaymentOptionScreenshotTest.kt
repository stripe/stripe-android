
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

    private fun paymentOptionScreenshotTestHelper(
        isSelected: Boolean,
        shouldShowModifyBadge: Boolean,
        shouldShowDefaultBadge: Boolean,
        isEnabled: Boolean,
    ) {
        paparazziRule.snapshot {
            SavedPaymentMethodTab(
                isSelected = isSelected,
                shouldShowModifyBadge = shouldShowModifyBadge,
                shouldShowDefaultBadge = shouldShowDefaultBadge,
                isEnabled = isEnabled,
                viewWidth = 160.dp,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testEnabled() {
        paymentOptionScreenshotTestHelper(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testDisabled() {
        paymentOptionScreenshotTestHelper(
            isSelected = false,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = false,
        )
    }

    @Test
    fun testSelected() {
        paymentOptionScreenshotTestHelper(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testSelectedAndDisabled() {
        paymentOptionScreenshotTestHelper(
            isSelected = true,
            shouldShowModifyBadge = false,
            shouldShowDefaultBadge = false,
            isEnabled = false,
        )
    }

    @Test
    fun testModifying() {
        paymentOptionScreenshotTestHelper(
            isSelected = false,
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = false,
            isEnabled = true,
        )
    }

    @Test
    fun testDefaultEditing() {
        paymentOptionScreenshotTestHelper(
            isSelected = false,
            shouldShowModifyBadge = true,
            shouldShowDefaultBadge = true,
            isEnabled = true,
        )
    }
}
