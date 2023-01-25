package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class PaymentSheetTopBarScreenshotTest {

    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        FontSize.values(),
        PaymentSheetAppearance.values(),
        padding = PaddingValues(0.dp),
    )

    @Test
    fun testLoading() {
        val state = PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_close,
            contentDescription = R.string.close,
            showTestModeLabel = false,
            showEditMenu = false,
            editMenuLabel = R.string.edit,
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
            contentDescription = R.string.close,
            showTestModeLabel = true,
            showEditMenu = true,
            editMenuLabel = R.string.edit,
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
            contentDescription = R.string.close,
            showTestModeLabel = true,
            showEditMenu = true,
            editMenuLabel = R.string.done,
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
            contentDescription = R.string.back,
            showTestModeLabel = true,
            showEditMenu = false,
            editMenuLabel = R.string.edit,
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
