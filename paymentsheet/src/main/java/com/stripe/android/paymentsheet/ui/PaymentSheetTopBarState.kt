package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen

internal data class PaymentSheetTopBarState(
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int,
    val showTestModeLabel: Boolean,
    val showEditMenu: Boolean,
    @StringRes val editMenuLabel: Int,
    val isEnabled: Boolean,
)

@Composable
internal fun rememberPaymentSheetTopBarState(
    screen: PaymentSheetScreen,
    paymentMethods: List<PaymentMethod>?,
    isLiveMode: Boolean,
    isProcessing: Boolean,
    isEditing: Boolean,
): PaymentSheetTopBarState {
    return remember(screen, paymentMethods, isLiveMode, isProcessing, isEditing) {
        val icon = if (screen == PaymentSheetScreen.AddAnotherPaymentMethod) {
            R.drawable.stripe_ic_paymentsheet_back
        } else {
            R.drawable.stripe_ic_paymentsheet_close
        }

        val contentDescription = if (screen == PaymentSheetScreen.AddAnotherPaymentMethod) {
            R.string.back
        } else {
            R.string.stripe_paymentsheet_close
        }

        val showOptionsMenu = screen is PaymentSheetScreen.SelectSavedPaymentMethods

        val editMenuLabel = if (isEditing) {
            R.string.done
        } else {
            R.string.edit
        }

        PaymentSheetTopBarState(
            icon = icon,
            contentDescription = contentDescription,
            showTestModeLabel = !isLiveMode,
            showEditMenu = showOptionsMenu && !paymentMethods.isNullOrEmpty(),
            editMenuLabel = editMenuLabel,
            isEnabled = !isProcessing,
        )
    }
}
