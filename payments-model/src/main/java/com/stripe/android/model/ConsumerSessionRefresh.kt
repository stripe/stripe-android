package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The result of a call to refresh the [ConsumerSession] for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSessionRefresh(
    @SerialName("consumer_session")
    val consumerSession: ConsumerSession,
    @SerialName("link_auth_intent")
    val linkAuthIntent: LinkAuthIntent?,
) : StripeModel
