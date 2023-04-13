package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The result of a call to retrieve the [ConsumerSession] for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSessionLookup(
    @SerialName("exists")
    val exists: Boolean,
    @SerialName("consumer_session")
    val consumerSession: ConsumerSession? = null,
    @SerialName("error_message")
    val errorMessage: String? = null
) : StripeModel
