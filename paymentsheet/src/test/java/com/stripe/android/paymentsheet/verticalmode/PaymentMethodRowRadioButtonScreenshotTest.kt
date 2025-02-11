package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class PaymentMethodRowRadioButtonScreenshotTest {

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
                style = FlatWithRadio.defaultLight,
                shouldShowDefaultBadge = false,
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
                style = FlatWithRadio.defaultLight,
                shouldShowDefaultBadge = false,
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
                style = FlatWithRadio.defaultLight,
                shouldShowDefaultBadge = false,
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
                style = FlatWithRadio.defaultLight,
                shouldShowDefaultBadge = false,
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
                style = FlatWithRadio.defaultLight,
                shouldShowDefaultBadge = false,
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
                style = FlatWithRadio.defaultLight,
                trailingContent = {
                    Text(text = "View more")
                },
                shouldShowDefaultBadge = false,
            )
        }
    }

    @Test
    fun testStyleAppearance() {
        val style = FlatWithRadio(
            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
            separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
            selectedColor = StripeThemeDefaults.colorsLight.materialColors.error.toArgb(),
            unselectedColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
            additionalInsetsDp = 40f
        )
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
                style = style,
                trailingContent = {
                    Text(text = "View more")
                },
                shouldShowDefaultBadge = false,
            )
        }
    }
}
