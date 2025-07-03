package com.stripe.android.link

import com.stripe.android.link.LinkPaymentMethod.ConsumerPaymentDetails

/**
 * State container for Link payment details.
 * When null, payment details have not been loaded yet.
 * When non-null, contains the loaded payment details (which may be an empty list).
 */
internal data class ConsumerState(
    val paymentDetails: List<ConsumerPaymentDetails>
)
