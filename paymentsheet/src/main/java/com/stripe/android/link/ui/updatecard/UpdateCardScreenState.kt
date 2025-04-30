package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.CardUpdateParams

@Immutable
internal data class UpdateCardScreenState(
    val paymentDetailsId: String,
    val isDefault: Boolean = false,
    val cardUpdateParams: CardUpdateParams? = null,
    val preferredCardBrand: CardBrand? = null,
    val error: Throwable? = null,
    val processing: Boolean = false,
) {

    val cardModified: Boolean
        get() = cardUpdateParams != null

    val primaryButtonState: PrimaryButtonState
        get() = when {
            cardModified.not() -> PrimaryButtonState.Disabled
            processing -> PrimaryButtonState.Processing
            else -> PrimaryButtonState.Enabled
        }

    val errorMessage: ResolvableString?
        get() = error?.stripeErrorMessage()
}
