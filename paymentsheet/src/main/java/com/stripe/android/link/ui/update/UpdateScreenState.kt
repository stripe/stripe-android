package com.stripe.android.link.ui.update

import androidx.compose.runtime.Immutable

@Immutable
internal data class UpdateScreenState(
    val paymentDetailsId: String,
) {

    internal companion object {
        fun create(
            paymentDetailsId: String
        ): UpdateScreenState {
            return UpdateScreenState(
                paymentDetailsId = paymentDetailsId
            )
        }
    }
}
