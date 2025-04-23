package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.CardUpdateParams

@Immutable
internal data class UpdateCardScreenState(
    val payload: Payload? = null,
    val cardUpdateParams: CardUpdateParams? = null,
) {
    data class Payload(
        val paymentDetails: ConsumerPaymentDetails.Card
    )
}
