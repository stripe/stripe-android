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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton
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

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class PaymentMethodRowFloatingButtonScreenshotTest {

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
        testPaymentMethodRowButton_FloatingButton()
    }

    @Test
    fun testDisabledState() {
        testPaymentMethodRowButton_FloatingButton(
            isEnabled = false,
        )
    }

    @Test
    fun testSelectedState() {
        testPaymentMethodRowButton_FloatingButton(
            isSelected = true,
        )
    }

    @Test
    fun testMultilineText() {
        testPaymentMethodRowButton_FloatingButton(
            subtitle = "Please click me, I'm fancy",
        )
    }

    @Test
    fun testMultilineTextTruncation() {
        testPaymentMethodRowButton_FloatingButton(
            subtitle = "Please click me, I'm fancy, but I shouldn't extend a a a a a a a a a a a a a a a a " +
                "forever.",
        )
    }

    @Test
    fun testTrailingContent() {
        testPaymentMethodRowButton_FloatingButton(
            trailingContent = {
                TrailingContent()
            }
        )
    }

    @Test
    fun testPromoText() {
        testPaymentMethodRowButton_FloatingButton(
            iconContent = {
                DefaultPaymentMethodRowIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank
                )
            },
            trailingContent = {
                TrailingContent()
            },
            title = "US Bank Account",
            promoText = "$5",
        )
    }

    @Test
    fun testPromoTextDisabled() {
        testPaymentMethodRowButton_FloatingButton(
            isEnabled = false,
            iconContent = {
                DefaultPaymentMethodRowIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank
                )
            },
            trailingContent = {
                TrailingContent()
            },
            title = "US Bank Account",
            promoText = "$5",
        )
    }

    @Test
    fun testStyleAppearance() {
        val style = FloatingButton(
            spacingDp = StripeThemeDefaults.floating.spacing,
            additionalInsetsDp = 40f
        )
        testPaymentMethodRowButton_FloatingButton(
            trailingContent = {
                TrailingContent()
            },
            rowStyle = style,
        )
    }

    @Test
    fun testDefault() {
        testPaymentMethodRowButton_FloatingButton(
            shouldShowDefaultBadge = true,
        )
    }

    @Test
    fun testNonTintedIcon() {
        testPaymentMethodRowButton_FloatingButton(
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
        testPaymentMethodRowButton_FloatingButton(
            iconContent = {
                DefaultPaymentMethodRowIcon(
                    iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna
                )
            },
            title = "Buy now or pay later with Klarna"
        )
    }

    @Composable
    private fun TrailingContent() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .testTag(TEST_TAG_VIEW_MORE)
                .padding(vertical = 4.dp)
                .wrapContentHeight()
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

    private fun testPaymentMethodRowButton_FloatingButton(
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        iconContent: @Composable RowScope.() -> Unit = { DefaultPaymentMethodRowIcon() },
        rowStyle: PaymentSheet.Appearance.Embedded.RowStyle = FloatingButton.default,
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
            rowStyle = rowStyle,
            paparazziRule = paparazziRule
        )
    }
}
