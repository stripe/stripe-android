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
            showEditMenu = false,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = null,
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreen() {
        val state = PaymentSheetTopBarState(
            showEditMenu = true,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = null,
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testPaymentMethodsScreenEditing() {
        val state = PaymentSheetTopBarState(
            showEditMenu = true,
            isEditing = true,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = null,
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testAddPaymentMethodScreen() {
        val state = PaymentSheetTopBarState(
            showEditMenu = false,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = null,
                canNavigateBack = true,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testCloseWithTitle() {
        val state = PaymentSheetTopBarState(
            showEditMenu = false,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = "Add payment info",
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testBackWithTitleAndEditMenu() {
        val state = PaymentSheetTopBarState(
            showEditMenu = true,
            isEditing = false,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = "Manage cards",
                canNavigateBack = true,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }

    @Test
    fun testLongTitleWithEditMenu() {
        val state = PaymentSheetTopBarState(
            showEditMenu = true,
            isEditing = true,
            onEditIconPressed = {},
        )

        paparazzi.snapshot {
            PaymentSheetTopBar(
                state = state,
                title = "Select your payment method from the options below",
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = {},
            )
        }
    }
}
