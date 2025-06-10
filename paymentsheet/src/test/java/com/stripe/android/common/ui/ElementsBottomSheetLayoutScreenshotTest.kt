package com.stripe.android.common.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import org.junit.Rule
import org.junit.Test

class ElementsBottomSheetLayoutScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxSize(),
    )

    @Test
    fun testExpanded() {
        paparazzi.snapshot {
            Text(
                text = "My screen",
                modifier = Modifier.padding(20.dp)
            )

            ElementsBottomSheetLayout(
                state = rememberStripeBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                ),
                onDismissed = {},
            ) {
                Text(
                    text = "My bottom sheet",
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }

    private enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {
        DefaultAppearance(appearance = PaymentSheet.Appearance()),
        AppearanceWithCustomRadiusForBottomSheet(
            appearance = PaymentSheet.Appearance(
                shapes = DefaultAppearance.appearance.shapes.copy(
                    bottomSheetCornerRadiusDp = 50f,
                )
            )
        );

        override fun initialize() {
            appearance.parseAppearance()
        }

        override fun reset() {
            DefaultAppearance.appearance.parseAppearance()
        }
    }
}
