package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The result of a call to retrieve the [ConsumerSession] for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSessionLookup(
    val exists: Boolean,
    val consumerSession: ConsumerSession?,
    val errorMessage: String?
) : StripeModel
