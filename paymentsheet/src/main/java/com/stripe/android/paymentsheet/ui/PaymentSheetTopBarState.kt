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
    val onEditIconPressed: () -> Unit,
) {
    sealed interface Editable {
        data object Never : Editable
        data class Maybe(
            val isEditing: Boolean,
            val canEdit: Boolean,
            val onEditIconPressed: () -> Unit,
        ) : Editable
    }
}

internal object PaymentSheetTopBarStateFactory {
    fun create(
        hasBackStack: Boolean,
        isLiveMode: Boolean,
        editable: PaymentSheetTopBarState.Editable,
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

        val editMenuLabel = if ((editable as? PaymentSheetTopBarState.Editable.Maybe)?.isEditing == true) {
            StripeR.string.stripe_done
        } else {
            StripeR.string.stripe_edit
        }

        return PaymentSheetTopBarState(
            icon = icon,
            contentDescription = contentDescription,
            showTestModeLabel = !isLiveMode,
            showEditMenu = (editable as? PaymentSheetTopBarState.Editable.Maybe)?.canEdit == true,
            editMenuLabel = editMenuLabel,
            onEditIconPressed = (editable as? PaymentSheetTopBarState.Editable.Maybe)?.onEditIconPressed
                ?: {},
        )
    }
}
