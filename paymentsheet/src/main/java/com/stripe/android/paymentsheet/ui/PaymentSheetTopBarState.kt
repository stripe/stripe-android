package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

internal data class PaymentSheetTopBarState(
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int,
    val showTestModeLabel: Boolean,
    val showEditMenu: Boolean,
    @StringRes val editMenuLabel: Int,
)

internal object PaymentSheetTopBarStateFactory {
    fun create(
        hasBackStack: Boolean,
        isLiveMode: Boolean,
        isEditing: Boolean,
        canEdit: Boolean,
    ): PaymentSheetTopBarState {
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

        return PaymentSheetTopBarState(
            icon = icon,
            contentDescription = contentDescription,
            showTestModeLabel = !isLiveMode,
            showEditMenu = canEdit,
            editMenuLabel = editMenuLabel,
        )
    }
}
