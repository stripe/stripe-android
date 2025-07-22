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
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class PaymentMethodRowDisclosureButtonScreenshotTest {
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
        testPaymentMethodRowButton_Disclosure()
    }

    @Test
    fun testDisabledState() {
        testPaymentMethodRowButton_Disclosure(
            isEnabled = false,
        )
    }

    @Test
    fun testSelectedState() {
        testPaymentMethodRowButton_Disclosure(
            isSelected = true,
        )
    }

    @Test
    fun testMultilineText() {
        testPaymentMethodRowButton_Disclosure(
            subtitle = "Please click me, I'm fancy",
        )
    }

    @Test
    fun testMultilineTextTruncation() {
        testPaymentMethodRowButton_Disclosure(
            subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                "forever.",
        )
    }

    @Test
    fun testTrailingContent() {
        testPaymentMethodRowButton_Disclosure(
            trailingContent = {
                Text("View more")
            },
        )
    }

    @Test
    fun testStyleAppearance() {
        val style = FlatWithDisclosure.Builder()
            .separatorThicknessDp(StripeThemeDefaults.flat.separatorThickness)
            .startSeparatorInsetDp(StripeThemeDefaults.flat.separatorInsets)
            .endSeparatorInsetDp(StripeThemeDefaults.flat.separatorInsets)
            .topSeparatorEnabled(StripeThemeDefaults.flat.topSeparatorEnabled)
            .bottomSeparatorEnabled(StripeThemeDefaults.flat.bottomSeparatorEnabled)
            .additionalVerticalInsetsDp(40f)
            .horizontalInsetsDp(40f)
            .colorsLight(
                FlatWithDisclosure.Colors(
                    separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                    disclosureColor = StripeThemeDefaults.colorsLight.materialColors.error.toArgb()
                )
            )
            .colorsDark(
                FlatWithDisclosure.Colors(
                    separatorColor = StripeThemeDefaults.colorsDark.componentBorder.toArgb(),
                    disclosureColor = StripeThemeDefaults.colorsDark.materialColors.error.toArgb()
                )
            )
            .build()
        testPaymentMethodRowButton_Disclosure(
            appearance = PaymentSheet.Appearance.Embedded(style),
            trailingContent = {
                Text("View more")
            },
        )
    }

    @Test
    fun testNonTintedIcon() {
        testPaymentMethodRowButton_Disclosure(
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
        testPaymentMethodRowButton_Disclosure(
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
        testPaymentMethodRowButton_Disclosure(
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithDisclosure.default)
                .paymentMethodIconMargins(
                    PaymentSheet.Insets(10f, 10f, 10f, 10f)
                )
                .build()
        )
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun testTitleFont() {
        testPaymentMethodRowButton_Disclosure(
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(FlatWithDisclosure.default)
                .titleFont(
                    PaymentSheet.Typography.Font(
                        fontFamily = com.stripe.android.paymentsheet.R.font.cursive,
                        fontSizeSp = 20f,
                        fontWeight = 30,
                        letterSpacingSp = 10f
                    )
                )
                .build()
        )
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun testDisclosureIcon() {
        testPaymentMethodRowButton_Disclosure(
            appearance = PaymentSheet.Appearance.Embedded.Builder()
                .rowStyle(
                    rowStyle = FlatWithDisclosure.Builder()
                        .disclosureIconRes(com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_add_dark)
                        .build()
                )
                .build()
        )
    }

    private fun testPaymentMethodRowButton_Disclosure(
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        iconContent: @Composable RowScope.() -> Unit = {
            DefaultPaymentMethodRowIcon()
        },
        appearance: PaymentSheet.Appearance.Embedded = PaymentSheet.Appearance.Embedded(FlatWithDisclosure.default),
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
