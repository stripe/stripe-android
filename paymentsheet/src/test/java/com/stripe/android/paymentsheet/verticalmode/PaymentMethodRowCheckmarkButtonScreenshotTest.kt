package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                style = FlatWithCheckmark.defaultLight,
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
                style = FlatWithCheckmark.defaultLight,
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
                style = FlatWithCheckmark.defaultLight,
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
                style = FlatWithCheckmark.defaultLight,
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
                style = FlatWithCheckmark.defaultLight,
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
                style = FlatWithCheckmark.defaultLight,
                trailingContent = {
                    TrailingContent()
                },
                shouldShowDefaultBadge = false,
            )
        }
    }

    @Test
    fun testStyleAppearance() {
        val style = FlatWithCheckmark(
            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
            separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
            checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.error.toArgb(),
            checkmarkInsetDp = 20f,
            additionalVerticalInsetsDp = 40f,
            horizontalInsetsDp = 40f
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
                    TrailingContent()
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

    @Composable
    private fun TrailingContent() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 4.dp, end = 0.dp, top = 4.dp, bottom = 4.dp)
                .offset(x = 9.dp)
        ) {
            Text(
                stringResource(id = com.stripe.android.paymentsheet.R.string.stripe_view_more),
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(22.dp)
            )
        }
    }
}
