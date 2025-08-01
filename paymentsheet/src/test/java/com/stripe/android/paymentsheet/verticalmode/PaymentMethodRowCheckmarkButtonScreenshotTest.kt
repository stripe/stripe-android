package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class PaymentMethodRowCheckmarkButtonScreenshotTest {

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
        testPaymentMethodRowButton_Checkmark()
    }

    @Test
    fun testDisabledState() {
        testPaymentMethodRowButton_Checkmark(
            isEnabled = false,
        )
    }

    @Test
    fun testSelectedState() {
        testPaymentMethodRowButton_Checkmark(
            isSelected = true,
        )
    }

    @Test
    fun testMultilineText() {
        testPaymentMethodRowButton_Checkmark(
            subtitle = "Please click me, I'm fancy",
        )
    }

    @Test
    fun testMultilineTextTruncation() {
        testPaymentMethodRowButton_Checkmark(
            subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                "forever.",
        )
    }

    @Test
    fun testTrailingContent() {
        testPaymentMethodRowButton_Checkmark(
            trailingContent = {
                Text("View more")
            },
        )
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

        testPaymentMethodRowButton_Checkmark(
            appearance = PaymentSheet.Appearance.Embedded(style),
            trailingContent = {
                Text("View more")
            },
        )
    }

    @Test
    fun testNonTintedIcon() {
        testPaymentMethodRowButton_Checkmark(
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
        testPaymentMethodRowButton_Checkmark(
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
        testPaymentMethodRowButton_Checkmark(
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithCheckmark.default)
                .paymentMethodIconMargins(
                    PaymentSheet.Insets(10f, 10f, 10f, 10f)
                )
                .build()
        )
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun testFonts() {
        testPaymentMethodRowButton_Checkmark(
            subtitle = "this is a subtitle",
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithCheckmark.default)
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

    private fun testPaymentMethodRowButton_Checkmark(
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        iconContent: @Composable RowScope.() -> Unit = {
            DefaultPaymentMethodRowIcon()
        },
        appearance: PaymentSheet.Appearance.Embedded = PaymentSheet.Appearance.Embedded(FlatWithCheckmark.default),
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
