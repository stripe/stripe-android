package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class PaymentMethodRowRadioButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    )

    @Test
    fun testInitialState() {
        testPaymentMethodRowButton_RadioButton()
    }

    @Test
    fun testDisabledState() {
        testPaymentMethodRowButton_RadioButton(
            isEnabled = false,
        )
    }

    @Test
    fun testSelectedState() {
        testPaymentMethodRowButton_RadioButton(
            isSelected = false,
        )
    }

    @Test
    fun testMultilineText() {
        testPaymentMethodRowButton_RadioButton(
            subtitle = "Please click me, I'm fancy",
        )
    }

    @Test
    fun testMultilineTextTruncation() {
        testPaymentMethodRowButton_RadioButton(
            subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                "forever.",
        )
    }

    @Test
    fun testTrailingContent() {
        testPaymentMethodRowButton_RadioButton(
            trailingContent = {
                TrailingContent()
            },
        )
    }

    @Test
    fun testStyleAppearance() {
        val style = FlatWithRadio.Builder()
            .separatorThicknessDp(StripeThemeDefaults.flat.separatorThickness)
            .startSeparatorInsetDp(StripeThemeDefaults.flat.separatorInsets)
            .endSeparatorInsetDp(StripeThemeDefaults.flat.separatorInsets)
            .topSeparatorEnabled(StripeThemeDefaults.flat.topSeparatorEnabled)
            .bottomSeparatorEnabled(StripeThemeDefaults.flat.bottomSeparatorEnabled)
            .additionalVerticalInsetsDp(40f)
            .horizontalInsetsDp(40f)
            .colorsLight(
                FlatWithRadio.Colors(
                    separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                    selectedColor = StripeThemeDefaults.colorsLight.materialColors.error.toArgb(),
                    unselectedColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb()
                )
            )
            .colorsDark(
                FlatWithRadio.Colors(
                    separatorColor = StripeThemeDefaults.colorsDark.componentBorder.toArgb(),
                    selectedColor = StripeThemeDefaults.colorsDark.materialColors.error.toArgb(),
                    unselectedColor = StripeThemeDefaults.colorsDark.materialColors.primary.toArgb()
                )
            )
            .build()

        testPaymentMethodRowButton_RadioButton(
            trailingContent = {
                TrailingContent()
            },
            appearance = PaymentSheet.Appearance.Embedded(style),
        )
    }

    @Test
    fun testNonTintedIcon() {
        testPaymentMethodRowButton_RadioButton(
            iconContent = {
                DefaultPaymentMethodRowIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna
                )
            },
            title = "Klarna"
        )
    }

    @Test
    fun testLongTitle() {
        testPaymentMethodRowButton_RadioButton(
            iconContent = {
                DefaultPaymentMethodRowIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna
                )
            },
            title = "Buy now or pay later with Klarna"
        )
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun testIconMargins() {
        testPaymentMethodRowButton_RadioButton(
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithRadio.default)
                .paymentMethodIconMargins(
                    PaymentSheet.Insets(10f, 10f, 10f, 10f)
                )
                .build()
        )
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun testFonts() {
        testPaymentMethodRowButton_RadioButton(
            subtitle = "this is a subtitle",
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithRadio.default)
                .titleFont(
                    PaymentSheet.Typography.Font(
                        fontFamily = com.stripe.android.paymentsheet.R.font.cursive,
                        fontSizeSp = 20f,
                        fontWeight = 500,
                        letterSpacingSp = 10f
                    )
                )
                .subtitleFont(
                    PaymentSheet.Typography.Font(
                        fontFamily = com.stripe.android.paymentsheet.R.font.cursive,
                        fontSizeSp = 12f,
                        fontWeight = 200,
                        letterSpacingSp = 5f
                    )
                )
                .build()
        )
    }

    @Composable
    private fun TrailingContent() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .wrapContentHeight(),
        ) {
            Text(text = "View more")
            Icon(
                painter = painterResource(com.stripe.android.paymentsheet.R.drawable.stripe_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    .height(iconHeight).width(iconWidth)
                    .defaultMinSize(minWidth = iconWidth, minHeight = iconHeight)
            )
        }
    }

    private fun testPaymentMethodRowButton_RadioButton(
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        iconContent: @Composable RowScope.() -> Unit = { DefaultPaymentMethodRowIcon() },
        appearance: PaymentSheet.Appearance.Embedded = PaymentSheet.Appearance.Embedded(FlatWithRadio.default),
        trailingContent: @Composable RowScope.() -> Unit = {},
        title: String = "**** 4242",
        subtitle: String? = null,
        promoText: String? = null,
        shouldShowDefaultBadge: Boolean = false,
    ) {
        testPaymentMethodRowButton(
            isEnabled = isEnabled,
            isSelected = isSelected,
            iconContent = iconContent,
            title = title,
            subtitle = subtitle,
            promoText = promoText,
            trailingContent = trailingContent,
            shouldShowDefaultBadge = shouldShowDefaultBadge,
            appearance = appearance,
            paparazziRule = paparazziRule,
        )
    }
}
