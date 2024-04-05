
package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentOptionUi
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
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = false,
                editState = PaymentOptionEditState.None,
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
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = false,
                editState = PaymentOptionEditState.None,
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
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = true,
                editState = PaymentOptionEditState.None,
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
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = true,
                editState = PaymentOptionEditState.None,
                isEnabled = false,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testRemoving() {
        paparazziRule.snapshot {
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = false,
                editState = PaymentOptionEditState.Removable,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }

    @Test
    fun testConfirmRemoveDialog() {
        paparazziRule.snapshot {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                PaymentOptionUi(
                    viewWidth = 160.dp,
                    isSelected = false,
                    editState = PaymentOptionEditState.Removable,
                    isEnabled = true,
                    iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                    labelText = "••••4242",
                    description = "Description",
                    removePmDialogTitle = "Remove Dialog Title",
                    shouldOpenRemoveDialog = true,
                    onItemSelectedListener = {},
                    onRemoveListener = {},
                )
            }
        }
    }

    @Test
    fun testModifying() {
        paparazziRule.snapshot {
            PaymentOptionUi(
                viewWidth = 160.dp,
                isSelected = false,
                editState = PaymentOptionEditState.Modifiable,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                labelText = "••••4242",
                description = "Description",
                onItemSelectedListener = {},
            )
        }
    }
}
