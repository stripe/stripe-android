package com.stripe.android.financialconnections.model

import kotlinx.serialization.Serializable

/**
 * This is a mini-version of the `PaymentMethod` class in `payments-core`, which we don't have access to.
 */
@Serializable
internal data class PaymentMethod(
    val id: String,
)
