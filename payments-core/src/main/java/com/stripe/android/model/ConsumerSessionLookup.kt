package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * The result of a call to retrieve the [ConsumerSession] for a Link user.
 */
@Parcelize
data class ConsumerSessionLookup internal constructor(
    val exists: Boolean,
    val consumerSession: ConsumerSession?,
    val errorMessage: String?
) : StripeModel
