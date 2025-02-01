package com.stripe.android.link.ui.cardedit

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry

internal data class CardEditState(
    val paymentDetail: ConsumerPaymentDetails.Card? = null,
    val cardDetailsEntry: CardDetailsEntry? = null,
    val cardDefaultState: CardDefaultState? = null,
    val isUpdating: Boolean = false,
    val errorMessage: ResolvableString? = null
) {
    val primaryButtonState: PrimaryButtonState
        get() {
            if (isUpdating) return PrimaryButtonState.Processing
            if (cardDetailsEntry != null) return PrimaryButtonState.Enabled
            return PrimaryButtonState.Disabled
        }
}

internal data class CardDetailsEntry(
    val date: Pair<IdentifierSpec, FormFieldEntry>,
    val country: Pair<IdentifierSpec, FormFieldEntry>,
    val postalCode: Pair<IdentifierSpec, FormFieldEntry>,
    val cvc: Pair<IdentifierSpec, FormFieldEntry>
)

internal sealed interface CardDefaultState {
    data object CardIsDefault : CardDefaultState
    data class Value(val enabled: Boolean) : CardDefaultState
}
