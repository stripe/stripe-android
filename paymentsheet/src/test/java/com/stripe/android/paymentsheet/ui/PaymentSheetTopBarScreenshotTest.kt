package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

class PaymentSheetTopBarScreenshotTest {

    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        FontSize.values(),
        PaymentSheetAppearance.values(),
        boxModifier = Modifier.padding(0.dp).fillMaxWidth(),
    )

    @Test
    fun testLoading() {
        val state = PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_close,
            contentDescription = StripeR.string.stripe_close,
            showTestModeLabel = false,
            showEditMenu = false,
            editMenuLabel = StripeR.string.stripe_edit,
            isEnabled = true,
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                elevation = 0.dp,
                onNavigationIconPressed = {},
                onEditIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreen() {
        val state = PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_close,
            contentDescription = StripeR.string.stripe_close,
            showTestModeLabel = true,
            showEditMenu = true,
            editMenuLabel = StripeR.string.stripe_edit,
            isEnabled = true,
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                elevation = 0.dp,
                onNavigationIconPressed = {},
                onEditIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreenEditing() {
        val state = PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_close,
            contentDescription = StripeR.string.stripe_close,
            showTestModeLabel = true,
            showEditMenu = true,
            editMenuLabel = StripeR.string.stripe_done,
            isEnabled = true,
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                elevation = 0.dp,
                onNavigationIconPressed = {},
                onEditIconPressed = {},
            )
        }
    }

    @Test
    fun testAddPaymentMethodScreen() {
        val state = PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_back,
            contentDescription = StripeUiCoreR.string.stripe_back,
            showTestModeLabel = true,
            showEditMenu = false,
            editMenuLabel = StripeR.string.stripe_edit,
            isEnabled = true,
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                elevation = 0.dp,
                onNavigationIconPressed = {},
                onEditIconPressed = {},
            )
        }
    }
}
