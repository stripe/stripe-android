package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class PaymentSheetTopBarScreenshotTest {

    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        PaymentSheetAppearance.entries,
        boxModifier = Modifier.padding(0.dp).fillMaxWidth(),
    )

    @Test
    fun testLoading() {
        val state = PaymentSheetTopBarState(
            showTestModeLabel = false,
            showEditMenu = false,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                canNavigateBack = false,
                isEnabled = true,
                elevation = 0.dp,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreen() {
        val state = PaymentSheetTopBarState(
            showTestModeLabel = true,
            showEditMenu = true,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                canNavigateBack = false,
                isEnabled = true,
                elevation = 0.dp,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreenEditing() {
        val state = PaymentSheetTopBarState(
            showTestModeLabel = true,
            showEditMenu = true,
            isEditing = true,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                canNavigateBack = false,
                isEnabled = true,
                elevation = 0.dp,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testAddPaymentMethodScreen() {
        val state = PaymentSheetTopBarState(
            showTestModeLabel = true,
            showEditMenu = false,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                canNavigateBack = true,
                isEnabled = true,
                elevation = 0.dp,
                onNavigationIconPressed = {},
            )
        }
    }
}
