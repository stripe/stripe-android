package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class NewPaymentMethodRowButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testSelectedState_FloatingButton() {
        testNewPaymentMethodRowButton(
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )
    }

    @Test
    fun testSelectedState_FlatWithRadio() {
        testNewPaymentMethodRowButton(
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.default
        )
    }

    @Test
    fun testSelectedState_FlatWithCheckmark() {
        testNewPaymentMethodRowButton(
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.default
        )
    }

    @Test
    fun testUnselectedState() {
        testNewPaymentMethodRowButton(
            isSelected = false,
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )
    }

    @Test
    fun testDisabled() {
        testNewPaymentMethodRowButton(
            isEnabled = false,
            isSelected = false,
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )
    }

    @Test
    fun testLongTitle_FlatWithRadio() {
        testNewPaymentMethodRowButton(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
            isSelected = false,
            title = "The Greatest US Bank Account",
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.default
        )
    }

    @Test
    fun testLongTitle_FlatWithCheckmark() {
        testNewPaymentMethodRowButton(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
            isSelected = false,
            title = "The Greatest US Bank Account",
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.default
        )
    }

    @Test
    fun testLongTitle_FloatingButton() {
        testNewPaymentMethodRowButton(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_bank,
            isSelected = false,
            title = "The Greatest US Bank Account",
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )
    }

    @Test
    fun testSubtitle() {
        testNewPaymentMethodRowButton(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            iconRequiresTinting = false,
            isSelected = false,
            title = "Klarna",
            subtitle = "Buy now or pay later with Klarna.",
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )    }

    @Test
    fun testLongSubtitle() {
        testNewPaymentMethodRowButton(
            iconRes = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            iconRequiresTinting = false,
            isSelected = false,
            title = "Klarna",
            subtitle = "A very long subtitle, that you should read all of and you should Buy now or pay later" +
                " with Klarna.",
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
        )
    }

    private fun testNewPaymentMethodRowButton(
        isEnabled: Boolean = true,
        isSelected: Boolean = true,
        iconRes: Int = R.drawable.stripe_ic_paymentsheet_pm_card,
        iconRequiresTinting: Boolean = true,
        title: String = "Card",
        subtitle: String? = null,
        rowStyle: PaymentSheet.Appearance.Embedded.RowStyle,
    ) {
        paparazziRule.snapshot {
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = isSelected,
                iconRes = iconRes,
                iconUrl = null,
                imageLoader = Mockito.mock(),
                title = title,
                subtitle = subtitle,
                promoText = null,
                iconRequiresTinting = iconRequiresTinting,
                onClick = {},
                modifier = Modifier,
                rowStyle = rowStyle,
            )
        }
    }
}
