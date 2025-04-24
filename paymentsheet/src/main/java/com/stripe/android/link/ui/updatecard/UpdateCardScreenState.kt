package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable

@Immutable
internal data class UpdateCardScreenState(
    val paymentDetailsId: String,
) {

    internal companion object {
        fun create(
            paymentDetailsId: String
        ): UpdateCardScreenState {
            return UpdateCardScreenState(
                paymentDetailsId = paymentDetailsId
            )
        }
    }
}
