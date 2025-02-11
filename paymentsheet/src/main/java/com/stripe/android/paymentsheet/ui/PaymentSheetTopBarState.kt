package com.stripe.android.paymentsheet.ui

import androidx.annotation.StringRes
import com.stripe.android.R as StripeR

internal data class PaymentSheetTopBarState(
    val showTestModeLabel: Boolean,
    val showEditMenu: Boolean,
    val isEditing: Boolean,
    val onEditIconPressed: () -> Unit,
) {
    @get:StringRes val editMenuLabel: Int
        get() {
            return if (isEditing) {
                StripeR.string.stripe_done
            } else {
                StripeR.string.stripe_edit
            }
        }

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
        isLiveMode: Boolean,
        editable: PaymentSheetTopBarState.Editable,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarState(
            showTestModeLabel = !isLiveMode,
            showEditMenu = (editable as? PaymentSheetTopBarState.Editable.Maybe)?.canEdit == true,
            isEditing = (editable as? PaymentSheetTopBarState.Editable.Maybe)?.isEditing == true,
            onEditIconPressed = (editable as? PaymentSheetTopBarState.Editable.Maybe)?.onEditIconPressed
                ?: {},
        )
    }
}
