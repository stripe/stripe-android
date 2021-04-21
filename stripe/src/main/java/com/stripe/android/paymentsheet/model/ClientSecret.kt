package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.parcelize.Parcelize

/**
 * Represents the client secret for a [SetupIntent] or [PaymentIntent]
 */
internal sealed class ClientSecret : Parcelable {
    abstract val value: String
}

/**
 * Represents the client secret for a [PaymentIntent]
 */
@Parcelize
internal data class PaymentIntentClientSecret(
    override val value: String
) : ClientSecret()

/**
 * Represents the client secret for a [SetupIntent]
 */
@Parcelize
internal data class SetupIntentClientSecret(
    override val value: String
) : ClientSecret()
