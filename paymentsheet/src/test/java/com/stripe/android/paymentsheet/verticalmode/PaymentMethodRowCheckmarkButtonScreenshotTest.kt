package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class PaymentMethodRowCheckmarkButtonScreenshotTest {

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
                    Icon()
                },
                title = "**** 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
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
                    Icon()
                },
                title = "**** 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
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
                    Icon()
                },
                title = "**** 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
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
                    Icon()
                },
                title = "**** 4242",
                subtitle = "Please click me, I'm fancy",
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
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
                    Icon()
                },
                title = "**** 4242",
                subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                    "forever.",
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
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
                    Icon()
                },
                title = "**** 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = FlatWithCheckmark.default,
                trailingContent = {
                    Text("View more")
                },
                shouldShowDefaultBadge = false,
            )
        }
    }

    @Test
    fun testStyleAppearance() {
        val style = FlatWithCheckmark(
            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
            checkmarkInsetDp = 20f,
            additionalVerticalInsetsDp = 40f,
            horizontalInsetsDp = 40f,
            colorsLight = FlatWithCheckmark.Colors(
                separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.error.toArgb()
            ),
            colorsDark = FlatWithCheckmark.Colors(
                separatorColor = StripeThemeDefaults.colorsDark.componentBorder.toArgb(),
                checkmarkColor = StripeThemeDefaults.colorsDark.materialColors.error.toArgb()
            )
        )
        paparazziRule.snapshot {
            PaymentMethodRowButton(
                isEnabled = true,
                isSelected = false,
                iconContent = {
                    Icon()
                },
                title = "**** 4242",
                subtitle = null,
                promoText = null,
                onClick = {},
                style = style,
                trailingContent = {
                    Text("View more")
                },
                shouldShowDefaultBadge = false,
            )
        }
    }

    @Composable
    private fun Icon() {
        PaymentMethodIconFromResource(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_card,
            colorFilter = null,
            alignment = Alignment.Center,
            modifier = Modifier
                .height(iconHeight)
                .width(iconWidth)
        )
    }
}
