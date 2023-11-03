package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

internal data class PaymentSheetTopBarState(
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int,
    val showTestModeLabel: Boolean,
    val showEditMenu: Boolean,
    @StringRes val editMenuLabel: Int,
    val isEnabled: Boolean,
)

internal object PaymentSheetTopBarStateFactory {

    fun create(
        screen: PaymentSheetScreen,
        paymentMethods: List<PaymentMethod>,
        isLiveMode: Boolean,
        isProcessing: Boolean,
        isEditing: Boolean,
    ): PaymentSheetTopBarState {
        val hasBackStack = screen.canNavigateBack

        val icon = if (hasBackStack) {
            R.drawable.stripe_ic_paymentsheet_back
        } else {
            R.drawable.stripe_ic_paymentsheet_close
        }

        val contentDescription = if (hasBackStack) {
            StripeUiCoreR.string.stripe_back
        } else {
            R.string.stripe_paymentsheet_close
        }

        val editMenuLabel = if (isEditing) {
            StripeR.string.stripe_done
        } else {
            StripeR.string.stripe_edit
        }

        val showEditMenu = screen is SelectSavedPaymentMethods && paymentMethods.isNotEmpty()

        return PaymentSheetTopBarState(
            icon = icon,
            contentDescription = contentDescription,
            showTestModeLabel = !isLiveMode,
            showEditMenu = showEditMenu,
            editMenuLabel = editMenuLabel,
            isEnabled = !isProcessing,
        )
    }

    fun createDefault(): PaymentSheetTopBarState {
        return create(
            screen = PaymentSheetScreen.Loading,
            paymentMethods = emptyList(),
            isLiveMode = true,
            isProcessing = false,
            isEditing = false,
        )
    }
}
