package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import org.junit.Rule
import org.junit.Test

internal class PaymentMethodRowButtonCheckmarkScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    )

    @Test
    fun testInitialState() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testDisabledState() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = false,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testSelectedState() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = true,
                isSelected = true,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testMultilineText() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = "Please click me, I'm fancy",
                onClick = {},
            )
        }
    }

    @Test
    fun testMultilineTextTruncation() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                    "forever.",
                onClick = {},
            )
        }
    }

    @Test
    fun testTailingContent() {
        paparazziRule.snapshot {
            PaymentMethodRowButtonCheckmark(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null
                    )
                },
                title = "**** 4242",
                subtitle = null,
                onClick = {},
                trailingContent = {
                    Text(text = "View more")
                }
            )
        }
    }
}
