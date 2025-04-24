package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.CardUpdateParams

@Immutable
internal data class UpdateCardScreenState(
    val paymentDetailsId: String? = null,
    val isDefault: Boolean = false,
    val cardUpdateParams: CardUpdateParams? = null,
    val preferredCardBrand: CardBrand? = null,
    val error: Throwable? = null,
    val loading: Boolean = false,
) {

    val primaryButtonState: PrimaryButtonState
        get() = if (loading) {
            PrimaryButtonState.Processing
        } else {
            PrimaryButtonState.Enabled
        }
}
