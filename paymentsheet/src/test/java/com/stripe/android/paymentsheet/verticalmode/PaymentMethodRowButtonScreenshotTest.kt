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

internal class PaymentMethodRowButtonScreenshotTest {

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
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testDisabledState() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testSelectedState() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testMultilineText() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testMultilineTextTruncation() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
            )
        }
    }

    @Test
    fun testTailingContent() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
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
                promoText = null,
                onClick = {},
                trailingContent = {
                    Text(text = "View more")
                }
            )
        }
    }

    @Test
    fun testPromoText() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_bank),
                        contentDescription = null
                    )
                },
                title = "Bank",
                subtitle = null,
                promoText = "$5",
                onClick = {},
            )
        }
    }

    @Test
    fun testPromoTextDisabled() {
        paparazziRule.snapshot {
            PaymentMethodRowButton(
                isEnabled = false,
                isSelected = false,
                iconContent = {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_paymentsheet_pm_bank),
                        contentDescription = null
                    )
                },
                title = "Bank",
                subtitle = null,
                promoText = "$5",
                onClick = {},
            )
        }
    }
}
