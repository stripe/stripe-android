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

    @Parcelize
    data class PaymentIntentClientSecret(
        override val value: String
    ) : ClientSecret()

    @Parcelize
    data class SetupIntentClientSecret(
        override val value: String
    ) : ClientSecret()
}
