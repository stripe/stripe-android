package com.stripe.android

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Represents an Ephemeral Key that can be used temporarily for API operations that typically
 * require a secret key.
 *
 * See [Using Android Standard UI Components - Prepare your API](https://stripe.com/docs/mobile/android/standard#prepare-your-api)
 * for more details on ephemeral keys.
 */
@Parcelize
data class EphemeralKey internal constructor(
    /**
     * Represents a customer id or issuing card id, depending on the context
     */
    internal val objectId: String,

    internal val created: Long,
    internal val expires: Long,
    internal val id: String,
    internal val isLiveMode: Boolean,
    internal val objectType: String,
    val secret: String,
    internal val type: String
) : StripeModel
