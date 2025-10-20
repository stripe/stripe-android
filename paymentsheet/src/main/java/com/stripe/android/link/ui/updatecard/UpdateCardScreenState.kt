package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.R

@Immutable
internal data class UpdateCardScreenState(
    val paymentDetailsId: String,
    val billingDetailsUpdateFlow: BillingDetailsUpdateFlow? = null,
    val primaryButtonLabel: ResolvableString,
    val isDefault: Boolean = false,
    val cardUpdateParams: CardUpdateParams? = null,
    val preferredCardBrand: CardBrand? = null,
    val error: ResolvableString? = null,
    val processing: Boolean = false,
) {

    private val readyToSubmit: Boolean
        get() = cardUpdateParams != null

    val isBillingDetailsUpdateFlow: Boolean
        get() = billingDetailsUpdateFlow != null

    val primaryButtonState: PrimaryButtonState
        get() = when {
            readyToSubmit.not() -> PrimaryButtonState.Disabled
            processing -> PrimaryButtonState.Processing
            else -> PrimaryButtonState.Enabled
        }

    val thisIsYourDefaultHint: ResolvableString?
        get() = if (isDefault && !isBillingDetailsUpdateFlow) {
            R.string.stripe_link_update_card_default_card.resolvableString
        } else {
            null
        }
}
