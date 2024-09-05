package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The result of a call to Link consumer sign up.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSessionSignup(
    @SerialName("consumer_session")
    val consumerSession: ConsumerSession,
    @SerialName("publishable_key")
    val publishableKey: String? = null,
) : StripeModel
