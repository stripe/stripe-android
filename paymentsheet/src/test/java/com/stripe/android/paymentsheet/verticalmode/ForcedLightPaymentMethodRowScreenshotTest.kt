package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
import com.stripe.android.paymentsheet.utils.ForcedLightAppearance
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class ForcedLightPaymentMethodRowScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        listOf(ForcedLightAppearance),
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testForcedLightRadioRow() {
        testPaymentMethodRowButton(
            isEnabled = true,
            isSelected = true,
            iconContent = { DefaultPaymentMethodRowIcon() },
            appearance = PaymentSheet.Appearance.Embedded(FlatWithRadio.default),
            trailingContent = {},
            title = "**** 4242",
            subtitle = "Visa",
            promoText = null,
            shouldShowDefaultBadge = false,
            paparazziRule = paparazziRule,
        )
    }
}
